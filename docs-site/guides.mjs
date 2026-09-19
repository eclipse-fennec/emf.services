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
  { file: 'OVERVIEW.md', slug: 'overview', title: 'Overview' },
  { file: 'GETTING_STARTED.md', slug: 'getting-started', title: 'Getting started' },
  { file: 'EVENTING.md', slug: 'eventing', title: 'Eventing' },
  { file: 'FINGERPRINTS.md', slug: 'fingerprints', title: 'Fingerprints (sd1/im1)' },
  { file: 'TRANSPORTS.md', slug: 'transports', title: 'Transports' },
  { file: 'CODE_GENERATION.md', slug: 'code-generation', title: 'Code generation' },
  { file: 'RSA.md', slug: 'rsa', title: 'Remote Service Admin' },
  { file: 'CLIENT_FRAMEWORK_GUIDE.md', slug: 'client-framework', title: 'Client framework (Java)' },
  { file: 'ACQUISITION.md', slug: 'acquisition', title: 'Discovery, Acquisition, Invocation' },
  { file: 'UPDATE_POLICY.md', slug: 'update-policy', title: 'Update policies' },
  { file: 'WIRE_FORMAT.md', slug: 'wire-format', title: 'Wire format reference' },
  { file: 'ARCHITECTURE.md', slug: 'architecture', title: 'Architecture (internal)' },
  { file: 'HARNESS.md', slug: 'harness', title: 'Cross-language harness' },
  { file: 'DEPLOYMENT.md', slug: 'deployment', title: 'Deployment (container image)' },
];

export const EXAMPLES = [
  { file: 'EXAMPLE_PAYMENT.md', slug: 'payment', title: 'The Payment example' },
];
