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

package org.eclipse.fennec.tags.service;

import java.util.List;

import org.eclipse.fennec.tags.EffectiveTag;
import org.eclipse.fennec.tags.MatchMode;
import org.eclipse.fennec.tags.RequestedTag;
import org.eclipse.fennec.tags.Tag;

/**
 * The tag overlay (#272): a tree of categories, put on things that live
 * elsewhere.
 *
 * <p>A tag is named by its path, the ids from the root down joined by
 * {@code '/'} ({@code payment/method/card}). Wherever a path is taken,
 * a path the tag had before a rename or move is taken too
 * ({@link Tag#getFormerPaths()}), so whoever named it by the old one
 * keeps working; what is answered is always the current one.
 *
 * <p>A target is a kind and a key ({@code contract}/{@code Payment}).
 * Which targets exist, and which tags they ask for themselves, comes from
 * the {@link TagSubjects} of their kind — this service keeps only the
 * tree and what an administrator decided.
 *
 * <p>Reads answer copies; nothing a caller holds changes under it.
 */
public interface TagService {

	/** The whole tree: copies of the roots with their descendants. */
	List<Tag> tags();

	/**
	 * Creates a tag, or changes name, description and selfAssignable of
	 * the one that is there.
	 *
	 * @param parentPath     the parent, {@code null} or empty for a root
	 * @param id             the new tag's id: no {@code '/'}, unique among
	 *                       its siblings
	 * @return the tag's path
	 * @throws UnknownTagException  when the parent does not exist
	 * @throws InvalidTagException  when the id is empty or contains a {@code '/'}
	 */
	String putTag(String parentPath, String id, String name, String description, boolean selfAssignable);

	/**
	 * Renames a tag, moves it under another parent, or both. The tag and
	 * every descendant remember the path they had.
	 *
	 * @param newParentPath the new parent, {@code null} or empty for a root
	 * @param newId         the new id, {@code null} to keep it
	 * @return the tag's new path
	 * @throws UnknownTagException when the tag or the new parent does not exist
	 * @throws InvalidTagException when the new id is invalid, already taken
	 *                             among the new siblings, or the tag would
	 *                             become its own descendant
	 */
	String moveTag(String path, String newParentPath, String newId);

	/**
	 * Removes a tag with its subtree and every assignment to any of them.
	 *
	 * @throws UnknownTagException when the tag does not exist
	 */
	void removeTag(String path);

	/**
	 * An administrator puts a tag on a target. Assigning it twice changes
	 * nothing. The target need not exist yet: the assignment waits for it.
	 *
	 * @throws UnknownTagException when the tag does not exist
	 */
	void assign(String path, String kind, String key);

	/**
	 * Takes an administrator's assignment away again; one that is not
	 * there changes nothing. Tags the target carries for another reason
	 * stay.
	 *
	 * @throws UnknownTagException when the tag does not exist
	 */
	void unassign(String path, String kind, String key);

	/**
	 * The keys of the targets of a kind that carry the tags, sorted. A tag
	 * counts for each of its ancestors, so asking for a parent finds what
	 * carries a child. Only targets their source knows right now are
	 * answered. A path no tag has matches nothing.
	 *
	 * @param mode {@link MatchMode#ANY}: at least one of the paths;
	 *             {@link MatchMode#ALL}: every one
	 */
	List<String> find(List<String> paths, MatchMode mode, String kind);

	/**
	 * The tags a target carries, each with where it comes from, sorted by
	 * path. A tag carried for two reasons is reported twice.
	 */
	List<EffectiveTag> tagsOf(String kind, String key);

	/**
	 * What targets asked for and may not give themselves — the tag does not
	 * exist, or it is not self-assignable — oldest first. A request whose
	 * tag became self-assignable since is not answered: the target carries
	 * it now.
	 */
	List<RequestedTag> requestedTags();

	/**
	 * An administrator grants a request: the tag is created where it is
	 * missing (with the parents it needs, none of them self-assignable)
	 * and assigned to the target, and the request is gone.
	 *
	 * @throws UnknownTagException when there is no such request
	 * @throws InvalidTagException when the path is not a valid one
	 */
	void acceptRequestedTag(String path, String kind, String key);

	/**
	 * An administrator turns a request down. Nothing is blocked: the
	 * request comes back with the next registration that asks for it.
	 *
	 * @throws UnknownTagException when there is no such request
	 */
	void discardRequestedTag(String path, String kind, String key);
}
