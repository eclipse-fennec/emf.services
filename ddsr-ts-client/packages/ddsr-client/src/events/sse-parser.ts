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

/**
 * Incremental Server-Sent-Events parser — the TS mirror of the
 * hand-rolled parser in the Java client's RestEventSource.
 *
 * Only `data:` lines matter: they are accumulated (one optional leading
 * space stripped, joined with '\n') and flushed on a blank line. All
 * other fields (`event:`, `id:`, `retry:`, comments) are read and
 * ignored — the payload itself carries the event type.
 */
export class SseParser {
  private buffer = '';
  private dataLines: string[] = [];

  /**
   * Feed a chunk of the stream; returns the data payloads of all events
   * completed by this chunk.
   */
  feed(chunk: string): string[] {
    this.buffer += chunk;
    const completed: string[] = [];

    for (;;) {
      const nl = this.buffer.indexOf('\n');
      if (nl < 0) break;
      let line = this.buffer.slice(0, nl);
      this.buffer = this.buffer.slice(nl + 1);
      if (line.endsWith('\r')) line = line.slice(0, -1);

      if (line === '') {
        if (this.dataLines.length > 0) {
          completed.push(this.dataLines.join('\n'));
          this.dataLines = [];
        }
        continue;
      }
      if (line.startsWith('data:')) {
        let value = line.slice('data:'.length);
        if (value.startsWith(' ')) value = value.slice(1);
        this.dataLines.push(value);
      }
      // every other field is ignored on purpose
    }
    return completed;
  }

  /** Drop any partially accumulated event (stream ended mid-event). */
  reset(): void {
    this.buffer = '';
    this.dataLines = [];
  }
}
