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

export { generate } from './generate.js';
export type { GenerateOptions, GenerateResult, Side } from './generate.js';
export { loadCatalog, interfacesOf } from './catalog.js';
export type { CatalogSource } from './catalog.js';
export { emitConsumer, emitProvider } from './emit.js';
export type { GeneratedFile } from './emit.js';
export { mapType, identifier } from './type-mapping.js';
export type { MappedType } from './type-mapping.js';
