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
  ddsr-codegen --binding <xmi> --out <dir> [options]

  --broker <url>     read the catalog from a running broker
  --catalog <file>   read it from a saved catalog document
  --binding <file>   render the contracts a TypeScriptBinding names, as it says
  --out <dir>        where the generated package is written
  --side <which>     consumer | provider | both        (default: both)
  --only <names>     comma-separated interface names   (default: all)
  --tsm              also write a TSM module per contract, for @inject
  --name <pkg>       npm name of the package
                     (default: the binding's targetPackage, else @ddsr/generated-stubs)
  --version <v>      version of the package             (default: 1.0.0)
`;

function main(argv: string[]): Promise<void> {
  const args = new Map<string, string>();
  const FLAGS = new Set(['tsm', 'help']);
  for (let at = 0; at < argv.length; ) {
    if (!argv[at].startsWith('--')) throw new Error(`unexpected argument: ${argv[at]}`);
    const key = argv[at].slice(2);
    if (FLAGS.has(key)) {
      args.set(key, 'true');
      at += 1;
    } else {
      args.set(key, argv[at + 1] ?? '');
      at += 2;
    }
  }

  if (args.has('help') || argv.length === 0) {
    process.stdout.write(USAGE);
    return Promise.resolve();
  }

  const broker = args.get('broker');
  const catalog = args.get('catalog');
  const binding = args.get('binding');
  const outDir = args.get('out');
  const given = [broker, catalog, binding].filter(entry => entry !== undefined).length;
  if (given !== 1) throw new Error('exactly one of --broker, --catalog or --binding is required');
  if (!outDir) throw new Error('--out is required');

  const source: CatalogSource | undefined = broker ? { url: broker } : catalog ? { file: catalog } : undefined;
  const only = args.get('only')?.split(',').map(entry => entry.trim()).filter(entry => entry !== '');

  return generate({
    source,
    binding,
    outDir,
    side: (args.get('side') as Side | undefined) ?? 'both',
    only,
    tsm: args.has('tsm'),
    packageName: args.get('name'),
    packageVersion: args.get('version'),
  }).then(result => {
    process.stdout.write(
      `${result.packageName}: ${result.interfaces.length} interface(s) — ${result.interfaces.join(', ')}\n`
      + result.files.map(file => `  ${file}`).join('\n') + '\n'
      + (result.kept.length > 0
        ? result.kept.map(file => `  ${file} (kept)`).join('\n') + '\n'
        : ''));
  });
}

main(process.argv.slice(2)).catch((error: unknown) => {
  process.stderr.write(`ddsr-codegen: ${error instanceof Error ? error.message : String(error)}\n`);
  process.exit(1);
});
