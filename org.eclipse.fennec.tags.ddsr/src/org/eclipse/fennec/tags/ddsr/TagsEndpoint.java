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

package org.eclipse.fennec.tags.ddsr;

import java.util.List;

import org.eclipse.fennec.tags.FoundTargets;
import org.eclipse.fennec.tags.MatchMode;
import org.eclipse.fennec.tags.TagRegistry;
import org.eclipse.fennec.tags.TagTarget;
import org.eclipse.fennec.tags.TaggedTarget;
import org.eclipse.fennec.tags.TagsFactory;
import org.eclipse.fennec.tags.service.TagService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@code Tags} contract ({@code tags-api.xmi}) over the tag service:
 * one public method per operation, named and ordered as the contract
 * says, which is all the generic distribution needs to serve it.
 *
 * <p>Answers are single documents — a registry holding only the tree or
 * only the open requests, the keys a query found, the tags of one target — because an answer on this
 * wire is one model object and the objects it needs.
 *
 * <p>Deliberately not a {@link TagService}: a dispatcher looks an
 * operation up on a public interface first, and {@code find} there
 * answers a list, not what the contract declares.
 */
@Component(service = TagsEndpoint.class, property = "ddsr.contract=Tags")
public final class TagsEndpoint {

	private final TagService tags;

	@Activate
	public TagsEndpoint(@Reference TagService tags) {
		this.tags = tags;
	}

	public TagRegistry tags() {
		TagRegistry tree = TagsFactory.eINSTANCE.createTagRegistry();
		tree.getTags().addAll(tags.tags());
		return tree;
	}

	public String putTag(String parentPath, String id, String name, String description, Boolean selfAssignable) {
		return tags.putTag(parentPath, id, name, description, Boolean.TRUE.equals(selfAssignable));
	}

	public String moveTag(String path, String newParentPath, String newId) {
		return tags.moveTag(path, newParentPath, newId);
	}

	public void removeTag(String path) {
		tags.removeTag(path);
	}

	public void assign(String path, String kind, String key) {
		tags.assign(path, kind, key);
	}

	public void unassign(String path, String kind, String key) {
		tags.unassign(path, kind, key);
	}

	public FoundTargets find(List<String> paths, MatchMode mode, String kind) {
		FoundTargets found = TagsFactory.eINSTANCE.createFoundTargets();
		found.setKind(kind);
		found.getKeys().addAll(tags.find(paths, mode == null ? MatchMode.ANY : mode, kind));
		return found;
	}

	public TagRegistry requestedTags() {
		TagRegistry open = TagsFactory.eINSTANCE.createTagRegistry();
		open.getRequests().addAll(tags.requestedTags());
		return open;
	}

	public void acceptRequestedTag(String path, String kind, String key) {
		tags.acceptRequestedTag(path, kind, key);
	}

	public void discardRequestedTag(String path, String kind, String key) {
		tags.discardRequestedTag(path, kind, key);
	}

	public TaggedTarget tagsOf(String kind, String key) {
		TaggedTarget tagged = TagsFactory.eINSTANCE.createTaggedTarget();
		TagTarget target = TagsFactory.eINSTANCE.createTagTarget();
		target.setKind(kind);
		target.setKey(key);
		tagged.setTarget(target);
		tagged.getTags().addAll(tags.tagsOf(kind, key));
		return tagged;
	}
}
