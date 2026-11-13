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

export { MqttEventSource, DEFAULT_TOPIC_PREFIX } from './mqtt-event-source.js';
export type { MqttEventSourceOptions, MqttClientLike } from './mqtt-event-source.js';
export { MqttFlavorPlugin } from './mqtt-flavor-plugin.js';
export type { MqttFlavorPluginOptions } from './mqtt-flavor-plugin.js';
export { MqttOperationServer } from './mqtt-operation-server.js';
export type { MqttOperationServerOptions, OperationHandlers } from './mqtt-operation-server.js';
export {
  requestTopicFor, replyBaseFor, namedReplyBaseFor, OWN_REPLY_BASE, qosFor,
  encodeRequest, decodeRequest, encodeResponse, decodeResponse, connectMqtt, connectMessaging,
  isAmqpFlavor, separatorFor,
} from './mqtt-rpc.js';
export { AmqpClient, connectAmqp, isAmqpUrl, DEFAULT_EXCHANGE } from './amqp-client.js';
export type { AmqpConnectOptions, AmqpChannelLike, AmqpConnectionLike } from './amqp-client.js';
export type { MqttRpcRequest, MqttRpcResponse, MqttRpcClientLike } from './mqtt-rpc.js';
export { operationHandlers } from './operation-target.js';
export type { OperationTarget, HandlersOptions } from './operation-target.js';
export {
  SESSION, SESSION_CHANNEL, SESSION_LEASE, REPLY_ERROR, SESSION_REPLY_PREFIX, DEFAULT_LEASE_MS, DEFAULT_STREAM_BUFFER,
  BoundedQueue, encodeControl, readSessionMessage, requestSession, respondSession, sessionChannelFor,
} from './messaging-session.js';
export type { OverflowPolicy, StreamBuffer, SessionMessage } from './messaging-session.js';
