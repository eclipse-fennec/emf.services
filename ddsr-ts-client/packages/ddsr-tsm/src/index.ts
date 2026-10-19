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

export { DdsrClientService, FlavorPluginService, remoteServiceId } from './contracts.js';
export { publishClient, publishFlavorPlugin, flavorPluginsFrom, bindFlavorPlugins } from './registry-wiring.js';
export type { FlavorSink } from './registry-wiring.js';
export { mirrorRemoteServices } from './remote-mirror.js';
export type { RemoteMirror, RemoteMirrorOptions } from './remote-mirror.js';
