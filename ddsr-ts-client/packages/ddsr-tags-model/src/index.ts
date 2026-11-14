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

import { TagsPackage } from './org/eclipse/fennec/tags/TagsPackage.js';

export * from './org/eclipse/fennec/tags/index.js';
export { tagPath, tagsByPath } from './paths.js';

// Registered on import, so an answer of the Tags contract can be read:
// generated stubs import the types only, and XMI is read through the
// package registry by nsURI.
void TagsPackage.eINSTANCE;
