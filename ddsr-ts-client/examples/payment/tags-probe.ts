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
 * Harness scenario W (#280): the tag service from TypeScript, through the
 * generated stubs (@ddsr/tags-stubs) and the tag model (@ddsr/tags-model).
 * The probe finds Tags by lookup — the broker announces it — reads the
 * tree, listens to the changes as an administrator, assigns a tag and
 * hears it, and queries what carries it.
 *
 *   BROKER_URL=http://localhost:8897/ddsr/rest pnpm exec tsx tags-probe.ts
 */

import { RestFlavorPlugin } from '@ddsr/flavor-rest';
import { DdsrClientImpl } from '@ddsr/client';
import { TagChangeKind, tagsByPath } from '@ddsr/tags-model';
import type { TagChange } from '@ddsr/tags-model';
import { getTags } from '@ddsr/tags-stubs';
import { brokerTokens, consumerId, nodeFetch } from './broker-auth';

const BROKER_URL = process.env.BROKER_URL ?? 'http://localhost:8887/ddsr/rest';

async function main(): Promise<void> {
  const client = DdsrClientImpl.create({
    brokerUrl: BROKER_URL,
    flavorPlugins: [new RestFlavorPlugin()],
    consumerId: consumerId('tags-probe'),
    tokenSupplier: brokerTokens(),
    fetchFn: nodeFetch(),
  });
  const failures: string[] = [];
  const check = (name: string, ok: boolean, detail: string) => {
    console.log(`  ${ok ? '✓' : '✗'} ${name}: ${detail}`);
    if (!ok) failures.push(name);
  };

  const tags = await getTags(client.consumer);
  if (!tags) throw new Error('no Tags announced at the broker');
  check('ts-tags-found', true, 'Tags found by lookup, bound by its sd1');

  const tree = tagsByPath((await tags.tags()).tags);
  check('ts-tags-tree', tree.has('payment/method/card'), `${tree.size} tag(s), payment/method/card among them`);

  // Listen first, then change: the broadcast has no replay.
  const heard: TagChange[] = [];
  const listening = (async () => {
    for await (const change of tags.changes()) {
      heard.push(change);
      if (change.kind === TagChangeKind.ASSIGNED) break;
    }
  })();
  await new Promise(resolve => setTimeout(resolve, 1000));
  await tags.assign('other', 'contract', 'Payment');
  await Promise.race([listening, new Promise(resolve => setTimeout(resolve, 15000))]);
  const assigned = heard.find(change => change.kind === TagChangeKind.ASSIGNED);
  check('ts-tags-changes', assigned?.path === 'other' && assigned.target?.key === 'Payment',
    assigned ? `heard ASSIGNED ${assigned.path} → ${assigned.target?.kind} ${assigned.target?.key}` : 'nothing heard in 15 s');

  const found = await tags.find(['other'], undefined, 'contract');
  check('ts-tags-find', [...found.keys].includes('Payment'), `find(other) = ${[...found.keys].join(', ')}`);
  const carried = await tags.tagsOf('contract', 'Payment');
  check('ts-tags-of', [...carried.tags].some(tag => tag.path === 'other'),
    `Payment carries ${[...carried.tags].map(tag => tag.path).join(', ')}`);

  await tags.unassign('other', 'contract', 'Payment');
  await client.close();
  console.log(failures.length === 0 ? 'PROBE_OK' : `PROBE_FAIL ${failures.join(', ')}`);
  process.exit(failures.length === 0 ? 0 : 1);
}

main().catch(error => {
  console.log(`PROBE_FAIL ${String(error)}`);
  process.exit(1);
});
