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

import { describe, expect, it } from 'vitest';
import { SseParser } from '../src/events/sse-parser';

describe('SseParser', () => {
  it('flushes a data payload on the blank line', () => {
    const parser = new SseParser();
    expect(parser.feed('data: hello\n')).toEqual([]);
    expect(parser.feed('\n')).toEqual(['hello']);
  });

  it('joins multiple data lines with newline', () => {
    const parser = new SseParser();
    expect(parser.feed('data: line1\ndata: line2\n\n')).toEqual(['line1\nline2']);
  });

  it('strips exactly one leading space after the colon', () => {
    const parser = new SseParser();
    expect(parser.feed('data:  two spaces\n\n')).toEqual([' two spaces']);
    expect(parser.feed('data:nospace\n\n')).toEqual(['nospace']);
  });

  it('ignores event, id, retry and comment lines', () => {
    const parser = new SseParser();
    const events = parser.feed(
      'event: ddsr-service-event\nid: 7\nretry: 100\n: comment\ndata: payload\n\n'
    );
    expect(events).toEqual(['payload']);
  });

  it('handles chunks split mid-line and CRLF line endings', () => {
    const parser = new SseParser();
    expect(parser.feed('da')).toEqual([]);
    expect(parser.feed('ta: pay')).toEqual([]);
    expect(parser.feed('load\r\n\r\n')).toEqual(['payload']);
  });

  it('emits several events from one chunk', () => {
    const parser = new SseParser();
    expect(parser.feed('data: a\n\ndata: b\n\n')).toEqual(['a', 'b']);
  });

  it('a blank line without pending data emits nothing', () => {
    const parser = new SseParser();
    expect(parser.feed('\n\n\n')).toEqual([]);
  });

  it('reset drops a partially accumulated event', () => {
    const parser = new SseParser();
    parser.feed('data: partial\n');
    parser.reset();
    expect(parser.feed('\n')).toEqual([]);
  });
});
