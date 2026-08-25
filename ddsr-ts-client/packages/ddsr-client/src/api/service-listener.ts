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

import type { ServiceEvent } from '@ddsr/model';

/**
 * Application-facing service listener — the TS mirror of the Java
 * client's DdsrServiceListener. Deliberately NOT the model's
 * ServiceListener EClass (that stays a description); a plain function
 * keeps application code free of the reflective EMF API.
 *
 * Called on the transport's delivery task; must return fast and not
 * throw (a throwing listener is logged and does not stop the others).
 */
export type DdsrServiceListener = (event: ServiceEvent) => void;
