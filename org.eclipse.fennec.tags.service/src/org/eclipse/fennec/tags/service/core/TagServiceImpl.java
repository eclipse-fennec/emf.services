/********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Data In Motion Consulting - initial implementation
 ********************************************************************/


package org.eclipse.fennec.tags.service.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fennec.tags.EffectiveTag;
import org.eclipse.fennec.tags.MatchMode;
import org.eclipse.fennec.tags.Tag;
import org.eclipse.fennec.tags.TagAssignment;
import org.eclipse.fennec.tags.TagOrigin;
import org.eclipse.fennec.tags.TagRegistry;
import org.eclipse.fennec.tags.TagTarget;
import org.eclipse.fennec.tags.TagsFactory;
import org.eclipse.fennec.tags.service.InvalidTagException;
import org.eclipse.fennec.tags.service.TagService;
import org.eclipse.fennec.tags.service.TagStore;
import org.eclipse.fennec.tags.service.TagStoreException;
import org.eclipse.fennec.tags.service.TagSubjects;
import org.eclipse.fennec.tags.service.TagSubjects.Subject;
import org.eclipse.fennec.tags.service.UnknownTagException;

/**
 * The tag service over one {@link TagRegistry}, kept in a {@link TagStore}.
 *
 * <p>Every change runs under one write lock, is written before it is
 * acknowledged, and is rolled back when the write fails — the registry is
 * copied before the change and put back. Reads run under the read lock
 * and answer copies.
 *
 * <p>What a target carries is worked out when a query asks, from three
 * places: the administrator's assignments (kept here), the tags the target
 * asks for itself (from its {@link TagSubjects}, carried when the tag
 * exists and is self-assignable) and, later, the rules (#277). Nothing of
 * that is stored per target, so a tag created after a target arrived
 * applies to it at once.
 *
 * <p>Needs the Fennec OCL engine: a tag's path is derived in OCL
 * ({@link Tag#getPath()}), and EMF binds that derivation when a tag object
 * is created — the engine has to be registered before the first registry
 * is loaded.
 */
public final class TagServiceImpl implements TagService {

	private static final Logger LOG = Logger.getLogger(TagServiceImpl.class.getName());

	private final TagStore store;

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private final List<TagSubjects> sources = new CopyOnWriteArrayList<>();

	private TagRegistry registry;

	/**
	 * @throws TagStoreException when the store holds something that is not
	 *                           a tag registry, or cannot be read
	 */
	public TagServiceImpl(TagStore store) {
		this.store = store;
		try {
			this.registry = store.load();
		} catch (IOException readFailure) {
			throw new TagStoreException("the tag registry could not be read: " + readFailure.getMessage(), readFailure);
		}
	}

	/** A source of targets arrived. */
	public void addSubjects(TagSubjects subjects) {
		sources.add(subjects);
	}

	/** A source of targets went away; its targets are no longer answered. */
	public void removeSubjects(TagSubjects subjects) {
		sources.remove(subjects);
	}

	// ------------------------------------------------------------------
	// The tree
	// ------------------------------------------------------------------

	@Override
	public List<Tag> tags() {
		return read(() -> new ArrayList<>(EcoreUtil.copyAll(registry.getTags())));
	}

	@Override
	public String putTag(String parentPath, String id, String name, String description, boolean selfAssignable) {
		return write(() -> {
			Tag parent = isRoot(parentPath) ? null : require(parentPath);
			String checked = validId(id);
			Tag tag = sibling(parent, checked, null);
			if (tag == null) {
				tag = TagsFactory.eINSTANCE.createTag();
				tag.setId(checked);
				siblingsOf(parent).add(tag);
			}
			tag.setName(name);
			tag.setDescription(description);
			tag.setSelfAssignable(selfAssignable);
			return tag.getPath();
		});
	}

	@Override
	public String moveTag(String path, String newParentPath, String newId) {
		return write(() -> {
			Tag tag = require(path);
			Tag newParent = isRoot(newParentPath) ? null : require(newParentPath);
			for (Tag above = newParent; above != null; above = above.getParent()) {
				if (above == tag) {
					throw new InvalidTagException("'" + tag.getPath() + "' cannot move under itself");
				}
			}
			String id = newId == null || newId.isBlank() ? tag.getId() : validId(newId);
			if (sibling(newParent, id, tag) != null) {
				throw new InvalidTagException("'" + id + "' is taken under " + describe(newParent));
			}
			Map<Tag, String> before = new LinkedHashMap<>();
			for (Tag moving : subtree(tag)) {
				before.put(moving, moving.getPath());
			}
			siblingsOf(newParent).add(tag);
			tag.setId(id);
			before.forEach((moved, oldPath) -> {
				if (!oldPath.equals(moved.getPath()) && !moved.getFormerPaths().contains(oldPath)) {
					moved.getFormerPaths().add(oldPath);
				}
			});
			return tag.getPath();
		});
	}

	@Override
	public void removeTag(String path) {
		write(() -> {
			Tag tag = require(path);
			Set<Tag> going = new HashSet<>(subtree(tag));
			registry.getAssignments().removeIf(assignment -> going.contains(assignment.getTag()));
			registry.getRules().removeIf(rule -> going.contains(rule.getTag()));
			EcoreUtil.remove(tag);
			return null;
		});
	}

	// ------------------------------------------------------------------
	// Assignments
	// ------------------------------------------------------------------

	@Override
	public void assign(String path, String kind, String key) {
		write(() -> {
			Tag tag = require(path);
			if (assignment(tag, kind, key) == null) {
				TagAssignment assignment = TagsFactory.eINSTANCE.createTagAssignment();
				assignment.setTag(tag);
				assignment.setTarget(target(kind, key));
				assignment.setAssignedAt(new Date());
				registry.getAssignments().add(assignment);
			}
			return null;
		});
	}

	@Override
	public void unassign(String path, String kind, String key) {
		write(() -> {
			Tag tag = require(path);
			TagAssignment assignment = assignment(tag, kind, key);
			if (assignment != null) {
				registry.getAssignments().remove(assignment);
			}
			return null;
		});
	}

	// ------------------------------------------------------------------
	// Queries
	// ------------------------------------------------------------------

	@Override
	public List<String> find(List<String> paths, MatchMode mode, String kind) {
		return read(() -> {
			List<Tag> wanted = new ArrayList<>();
			for (String path : paths == null ? List.<String>of() : paths) {
				Tag tag = resolve(path);
				if (tag != null) {
					wanted.add(tag);
				} else if (mode == MatchMode.ALL) {
					return List.of();
				}
			}
			if (wanted.isEmpty()) {
				return List.of();
			}
			Set<String> keys = new TreeSet<>();
			for (Subject subject : subjects(kind).values()) {
				Set<Tag> carried = carried(subject.kind(), subject.key(), subject).keySet();
				boolean matches = mode == MatchMode.ALL
						? wanted.stream().allMatch(tag -> covers(tag, carried))
						: wanted.stream().anyMatch(tag -> covers(tag, carried));
				if (matches) {
					keys.add(subject.key());
				}
			}
			return List.copyOf(keys);
		});
	}

	@Override
	public List<EffectiveTag> tagsOf(String kind, String key) {
		return read(() -> {
			Subject subject = subjects(kind).get(key);
			List<EffectiveTag> effective = new ArrayList<>();
			carried(kind, key, subject).forEach((tag, origins) -> {
				for (TagOrigin origin : origins) {
					EffectiveTag one = TagsFactory.eINSTANCE.createEffectiveTag();
					one.setPath(tag.getPath());
					one.setOrigin(origin);
					effective.add(one);
				}
			});
			effective.sort(Comparator.comparing(EffectiveTag::getPath).thenComparing(EffectiveTag::getOrigin));
			return effective;
		});
	}

	// ------------------------------------------------------------------

	/** The tags a target carries, each with every reason it carries it for. */
	private Map<Tag, List<TagOrigin>> carried(String kind, String key, Subject subject) {
		Map<Tag, List<TagOrigin>> carried = new LinkedHashMap<>();
		for (TagAssignment assignment : registry.getAssignments()) {
			TagTarget target = assignment.getTarget();
			if (target != null && kind.equals(target.getKind()) && key.equals(target.getKey())) {
				carried.computeIfAbsent(assignment.getTag(), t -> new ArrayList<>()).add(TagOrigin.ADMIN);
			}
		}
		if (subject != null) {
			for (String requested : subject.requestedTags()) {
				Tag tag = resolve(requested);
				if (tag != null && tag.isSelfAssignable()) {
					carried.computeIfAbsent(tag, t -> new ArrayList<>()).add(TagOrigin.SOURCE);
				}
			}
		}
		return carried;
	}

	/** Whether the wanted tag is carried, itself or through a descendant. */
	private static boolean covers(Tag wanted, Collection<Tag> carried) {
		for (Tag tag : carried) {
			for (Tag above = tag; above != null; above = above.getParent()) {
				if (above == wanted) {
					return true;
				}
			}
		}
		return false;
	}

	/** The targets of a kind every source knows, by key; the first source to name a key wins. */
	private Map<String, Subject> subjects(String kind) {
		Map<String, Subject> byKey = new LinkedHashMap<>();
		for (TagSubjects source : sources) {
			try {
				if (source.kinds().contains(kind)) {
					for (Subject subject : source.subjects(kind)) {
						byKey.putIfAbsent(subject.key(), subject);
					}
				}
			} catch (RuntimeException sourceFailure) {
				// One source that cannot answer must not take the others along.
				LOG.log(Level.WARNING, "[Tags] a source of '" + kind + "' failed, answering without it", sourceFailure);
			}
		}
		return byKey;
	}

	/**
	 * The tag a path names: its current path first, a former one second.
	 * {@code null} when no tag has it.
	 */
	private Tag resolve(String path) {
		String normalized = normalize(path);
		if (normalized.isEmpty()) {
			return null;
		}
		Tag tag = null;
		EList<Tag> level = registry.getTags();
		for (String segment : normalized.split("/")) {
			tag = null;
			for (Tag candidate : level) {
				if (segment.equals(candidate.getId())) {
					tag = candidate;
					break;
				}
			}
			if (tag == null) {
				break;
			}
			level = tag.getChildren();
		}
		if (tag != null) {
			return tag;
		}
		for (Tag candidate : allTags()) {
			if (candidate.getFormerPaths().contains(normalized)) {
				return candidate;
			}
		}
		return null;
	}

	private Tag require(String path) {
		Tag tag = resolve(path);
		if (tag == null) {
			throw new UnknownTagException("no tag '" + normalize(path) + "'");
		}
		return tag;
	}

	private List<Tag> allTags() {
		List<Tag> all = new ArrayList<>();
		for (Tag root : registry.getTags()) {
			all.addAll(subtree(root));
		}
		return all;
	}

	private static List<Tag> subtree(Tag tag) {
		List<Tag> all = new ArrayList<>();
		all.add(tag);
		for (TreeIterator<EObject> contents = tag.eAllContents(); contents.hasNext();) {
			if (contents.next() instanceof Tag descendant) {
				all.add(descendant);
			}
		}
		return all;
	}

	private EList<Tag> siblingsOf(Tag parent) {
		return parent == null ? registry.getTags() : parent.getChildren();
	}

	/** The sibling with this id under the parent, other than {@code except}. */
	private Tag sibling(Tag parent, String id, Tag except) {
		for (Tag candidate : siblingsOf(parent)) {
			if (candidate != except && id.equals(candidate.getId())) {
				return candidate;
			}
		}
		return null;
	}

	private TagAssignment assignment(Tag tag, String kind, String key) {
		for (TagAssignment assignment : registry.getAssignments()) {
			TagTarget target = assignment.getTarget();
			if (assignment.getTag() == tag && target != null
					&& kind.equals(target.getKind()) && key.equals(target.getKey())) {
				return assignment;
			}
		}
		return null;
	}

	private static TagTarget target(String kind, String key) {
		if (kind == null || kind.isBlank() || key == null || key.isBlank()) {
			throw new InvalidTagException("a target needs a kind and a key");
		}
		TagTarget target = TagsFactory.eINSTANCE.createTagTarget();
		target.setKind(kind);
		target.setKey(key);
		return target;
	}

	private static String validId(String id) {
		if (id == null || id.isBlank()) {
			throw new InvalidTagException("a tag needs an id");
		}
		if (id.contains("/")) {
			throw new InvalidTagException("an id has no '/': '" + id + "'");
		}
		return id;
	}

	private static String describe(Tag parent) {
		return parent == null ? "the roots" : "'" + parent.getPath() + "'";
	}

	private static boolean isRoot(String parentPath) {
		return normalize(parentPath).isEmpty();
	}

	private static String normalize(String path) {
		if (path == null) {
			return "";
		}
		String trimmed = path.trim();
		while (trimmed.startsWith("/")) {
			trimmed = trimmed.substring(1);
		}
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	// ------------------------------------------------------------------
	// Locking and keeping
	// ------------------------------------------------------------------

	private <T> T read(Supplier<T> query) {
		lock.readLock().lock();
		try {
			return query.get();
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Runs a change, keeps it, and puts the registry back as it was when
	 * the change is refused or cannot be kept.
	 */
	private <T> T write(Supplier<T> change) {
		lock.writeLock().lock();
		TagRegistry before = EcoreUtil.copy(registry);
		try {
			T result = change.get();
			try {
				store.save(registry);
			} catch (IOException writeFailure) {
				registry = before;
				throw new TagStoreException("the change could not be kept and was rolled back: "
						+ writeFailure.getMessage(), writeFailure);
			}
			return result;
		} catch (RuntimeException refused) {
			registry = before;
			throw refused;
		} finally {
			lock.writeLock().unlock();
		}
	}
}
