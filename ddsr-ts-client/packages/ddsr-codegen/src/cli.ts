#!/usr/bin/env node
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

import { generate } from './generate.js';
import type { CatalogSource, Side } from './index.js';

const USAGE = `ddsr-codegen — typed stubs from a DDSR service catalog

  ddsr-codegen --broker <url>  --out <dir> [options]
  ddsr-codegen --catalog <xmi> --out <dir> [options]

  --broker <url>     read the catalog from a running broker
  --catalog <file>   read it from a saved catalog document
  --out <dir>        where the generated package is written
  --side <which>     consumer | provider | both        (default: both)
  --only <names>     comma-separated interface names   (default: all)
  --name <pkg>       npm name of the package    (default: @ddsr/generated-stubs)
  --version <v>      version of the package             (default: 1.0.0)
`;

function main(argv: string[]): Promise<void> {
  const args = new Map<string, string>();
  for (let at = 0; at < argv.length; at += 2) {
    if (!argv[at].startsWith('--')) throw new Error(`unexpected argument: ${argv[at]}`);
    args.set(argv[at].slice(2), argv[at + 1] ?? '');
  }

  if (args.has('help') || argv.length === 0) {
    process.stdout.write(USAGE);
    return Promise.resolve();
  }

  const broker = args.get('broker');
  const catalog = args.get('catalog');
  const outDir = args.get('out');
  if (!broker && !catalog) throw new Error('one of --broker or --catalog is required');
  if (broker && catalog) throw new Error('--broker and --catalog are mutually exclusive');
  if (!outDir) throw new Error('--out is required');

  const source: CatalogSource = broker ? { url: broker } : { file: catalog! };
  const only = args.get('only')?.split(',').map(entry => entry.trim()).filter(entry => entry !== '');

  return generate({
    source,
    outDir,
    side: (args.get('side') as Side | undefined) ?? 'both',
    only,
    packageName: args.get('name'),
    packageVersion: args.get('version'),
  }).then(result => {
    process.stdout.write(
      `${result.packageName}: ${result.interfaces.length} interface(s) — ${result.interfaces.join(', ')}\n`
      + result.files.map(file => `  ${file}`).join('\n') + '\n');
  });
}

main(process.argv.slice(2)).catch((error: unknown) => {
  process.stderr.write(`ddsr-codegen: ${error instanceof Error ? error.message : String(error)}\n`);
  process.exit(1);
});
