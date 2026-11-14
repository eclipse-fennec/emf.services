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

export { RestFlavorPlugin } from './rest-flavor-plugin.js';
export { serializeToXmi, deserializeFromXmi } from './xmi-support.js';
export { restDispatcher, writeAnswer } from './operation-dispatch.js';
export type { AnswerSink } from './operation-dispatch.js';
export { STREAM_MEDIA_TYPE, eventsOf, frame, framesOf, streams, valuesOf } from './rest-streams.js';
export type { StreamEvent } from './rest-streams.js';
export type { DispatchRequest, DispatchResult, DispatchOptions, OperationTarget } from './operation-dispatch.js';
