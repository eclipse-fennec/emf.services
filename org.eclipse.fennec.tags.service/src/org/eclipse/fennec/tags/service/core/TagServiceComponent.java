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

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.fennec.tags.EffectiveTag;
import org.eclipse.fennec.tags.MatchMode;
import org.eclipse.fennec.tags.RequestedTag;
import org.eclipse.fennec.tags.Tag;
import org.eclipse.fennec.tags.TagsPackage;
import org.eclipse.fennec.tags.service.TagService;
import org.eclipse.fennec.tags.service.TagSubjects;
import org.eclipse.fennec.tags.service.store.XmiTagStore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * The tag service as a component: one {@link TagServiceImpl} over an
 * {@link XmiTagStore}, fed by every {@link TagSubjects} there is.
 *
 * <p>Waits for the Fennec OCL engine: a tag's path is derived in OCL, and
 * EMF binds that derivation when a tag object is created — loading the
 * registry before the engine is registered would leave every tag without
 * a path for good. The factory is therefore a mandatory, static
 * reference, and it is put into EMF's global registry here as well, in
 * case the EMF whiteboard has not done so yet.
 *
 * <p>Sources of targets come and go: a dynamic, multiple reference, with
 * methods, as the rule for optional collaborators says.
 */
@Component(service = TagService.class, configurationPid = "org.eclipse.fennec.tags.service",
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = TagServiceComponent.Config.class)
public final class TagServiceComponent implements TagService {

	private static final Logger LOG = Logger.getLogger(TagServiceComponent.class.getName());

	/** The delegate URI tags.ecore names — the Fennec OCL engine. */
	static final String OCL_DELEGATE = "http://www.eclipse.org/fennec/m2x/ocl/1.0";

	@ObjectClassDefinition(name = "Fennec tag service", description = "The tag overlay and where it keeps its tags.")
	public @interface Config {

		@AttributeDefinition(name = "Store file",
				description = "The XMI file the tags are kept in; relative to the working directory.")
		String store_file() default "tags.xmi";
	}

	private final List<TagSubjects> early = new CopyOnWriteArrayList<>();

	private volatile TagServiceImpl service;

	@Activate
	public TagServiceComponent(Config config,
			@Reference(target = "(emf.configuratorName=" + OCL_DELEGATE + ")")
			EStructuralFeature.Internal.SettingDelegate.Factory ocl) {
		EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE.putIfAbsent(OCL_DELEGATE, ocl);
		// A tag object created before the engine was there bound "no
		// derivation" to the feature; forget that before anything loads.
		((EStructuralFeature.Internal) TagsPackage.Literals.TAG__PATH).setSettingDelegate(null);
		XmiTagStore store = new XmiTagStore(Path.of(config.store_file()));
		TagServiceImpl impl = new TagServiceImpl(store);
		early.forEach(impl::addSubjects);
		service = impl;
		LOG.info("[Tags] serving the tags kept in " + store.file().toAbsolutePath());
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addSubjects(TagSubjects subjects) {
		early.add(subjects);
		TagServiceImpl impl = service;
		if (impl != null) {
			impl.addSubjects(subjects);
		}
	}

	void removeSubjects(TagSubjects subjects) {
		early.remove(subjects);
		TagServiceImpl impl = service;
		if (impl != null) {
			impl.removeSubjects(subjects);
		}
	}

	@Override
	public List<Tag> tags() {
		return service.tags();
	}

	@Override
	public String putTag(String parentPath, String id, String name, String description, boolean selfAssignable) {
		return service.putTag(parentPath, id, name, description, selfAssignable);
	}

	@Override
	public String moveTag(String path, String newParentPath, String newId) {
		return service.moveTag(path, newParentPath, newId);
	}

	@Override
	public void removeTag(String path) {
		service.removeTag(path);
	}

	@Override
	public void assign(String path, String kind, String key) {
		service.assign(path, kind, key);
	}

	@Override
	public void unassign(String path, String kind, String key) {
		service.unassign(path, kind, key);
	}

	@Override
	public List<String> find(List<String> paths, MatchMode mode, String kind) {
		return service.find(paths, mode, kind);
	}

	@Override
	public List<EffectiveTag> tagsOf(String kind, String key) {
		return service.tagsOf(kind, key);
	}

	@Override
	public List<RequestedTag> requestedTags() {
		return service.requestedTags();
	}

	@Override
	public void acceptRequestedTag(String path, String kind, String key) {
		service.acceptRequestedTag(path, kind, key);
	}

	@Override
	public void discardRequestedTag(String path, String kind, String key) {
		service.discardRequestedTag(path, kind, key);
	}
}
