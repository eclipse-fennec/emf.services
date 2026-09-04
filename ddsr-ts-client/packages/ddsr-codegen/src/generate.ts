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

import { existsSync, mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import type { ServiceInterface } from '@ddsr/model';
import { loadCatalog } from './catalog.js';
import type { CatalogSource } from './catalog.js';
import { emitConsumer, emitProvider } from './emit.js';
import type { GeneratedFile } from './emit.js';
import { identifier } from './type-mapping.js';

export type Side = 'consumer' | 'provider' | 'both';

export interface GenerateOptions {
  source: CatalogSource;
  /** Directory the package is written to. */
  outDir: string;
  /** Which stubs to write; both by default. */
  side?: Side;
  /** Only these interfaces, by name; all of them by default. */
  only?: string[];
  /** npm name of the generated package. */
  packageName?: string;
  packageVersion?: string;
  fetchFn?: typeof fetch;
}

export interface GenerateResult {
  packageName: string;
  interfaces: string[];
  /** What this run wrote. */
  files: string[];
  /** Scaffolding left alone because it was already there. */
  kept: string[];
}

/**
 * Write a TypeScript package for the contracts in a catalog.
 *
 * A package rather than loose files, because that is what makes the
 * result usable the way anything else is: a consumer adds a dependency,
 * imports a name, and gets a type. Loose files in someone's source tree
 * have to be copied, and then they drift.
 */
export async function generate(options: GenerateOptions): Promise<GenerateResult> {
  const side = options.side ?? 'both';
  const packageName = options.packageName ?? '@ddsr/generated-stubs';
  const all = await loadCatalog(options.source, options.fetchFn ?? fetch);
  const wanted = options.only && options.only.length > 0
    ? all.filter(entry => options.only!.includes(entry.name ?? ''))
    : all;

  if (wanted.length === 0) {
    throw new Error(options.only
      ? `no ServiceInterface in the catalog matched: ${options.only.join(', ')}`
      : 'the catalog holds no ServiceInterface');
  }

  const files: GeneratedFile[] = [];
  for (const serviceInterface of wanted) {
    if (side === 'consumer' || side === 'both') files.push(emitConsumer(serviceInterface));
    if (side === 'provider' || side === 'both') files.push(emitProvider(serviceInterface));
  }
  files.push(indexFile(files));

  // The scaffolding is written once. A generated stub is replaced on
  // every run, but the package.json of a checked-in project carries
  // things this generator cannot know — a generate script, a place in
  // the workspace, dependencies someone added — and overwriting it
  // would take them away on the next regeneration.
  const scaffolding = [packageJson(packageName, options.packageVersion ?? '1.0.0'), tsconfig()];
  const written: string[] = [];
  const kept: string[] = [];

  for (const file of [...files, ...scaffolding]) {
    const path = join(options.outDir, file.path);
    const isScaffolding = scaffolding.includes(file);
    if (isScaffolding && existsSync(path)) {
      kept.push(file.path);
      continue;
    }
    mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path, file.content, 'utf8');
    written.push(file.path);
  }

  return {
    packageName,
    interfaces: wanted.map(entry => identifier(entry.name, 'Service')),
    files: written,
    kept,
  };
}

function indexFile(files: GeneratedFile[]): GeneratedFile {
  const exports = files
    .filter(file => file.path.startsWith('src/'))
    .map(file => `export * from './${file.path.replace(/^src\//, '').replace(/\.ts$/, '.js')}';`);
  return { path: 'src/index.ts', content: exports.join('\n') + '\n' };
}

function packageJson(name: string, version: string): GeneratedFile {
  return {
    path: 'package.json',
    content: JSON.stringify({
      name,
      version,
      type: 'module',
      main: 'dist/index.js',
      types: 'dist/index.d.ts',
      exports: { '.': { types: './dist/index.d.ts', import: './dist/index.js' } },
      scripts: { build: 'tsc' },
      dependencies: { '@ddsr/client': 'workspace:*', '@ddsr/model': 'workspace:*' },
      devDependencies: { typescript: '~5.8.0' },
    }, null, 2) + '\n',
  };
}

function tsconfig(): GeneratedFile {
  return {
    path: 'tsconfig.json',
    content: JSON.stringify({
      compilerOptions: {
        target: 'ES2022', module: 'ESNext', moduleResolution: 'bundler',
        declaration: true, strict: true, skipLibCheck: true,
        outDir: 'dist', rootDir: 'src',
      },
      include: ['src'],
    }, null, 2) + '\n',
  };
}
