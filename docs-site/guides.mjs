// The published, user-facing pages (allowlist). Shared by the sync script and the
// VitePress config so the set and its order are defined exactly once.
//   file  — source markdown in ../docs (the single source of truth)
//   slug  — route name under the section
//   title — sidebar / nav label
//
// Publication is deliberately an allowlist: internal design docs (OPEN_ISSUES,
// DECISIONS_PARITY, security analysis, plans) stay in ../docs and are browsed on
// GitHub. The full documentation buildout is tracked in issue #5.
export const GUIDES = [
  { file: 'GETTING_STARTED.md', slug: 'getting-started', title: 'Getting started' },
  { file: 'ARCHITECTURE.md', slug: 'architecture', title: 'Architecture' },
  { file: 'WIRE_FORMAT.md', slug: 'wire-format', title: 'Wire format reference' },
  { file: 'FINGERPRINTS.md', slug: 'fingerprints', title: 'Fingerprints (sd1/im1)' },
  { file: 'CLIENT_FRAMEWORK_GUIDE.md', slug: 'client-framework', title: 'Client framework (Java)' },
  { file: 'ACQUISITION.md', slug: 'acquisition', title: 'Discovery, Acquisition, Invocation' },
  { file: 'UPDATE_POLICY.md', slug: 'update-policy', title: 'Update policies' },
  { file: 'HARNESS.md', slug: 'harness', title: 'Cross-language harness' },
];

export const EXAMPLES = [
  { file: 'EXAMPLE_PAYMENT.md', slug: 'payment', title: 'The Payment example' },
];
