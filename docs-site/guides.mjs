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
  { file: 'REQUIREMENTS.md', slug: 'requirements', title: 'Vision & Requirements' },
  { file: 'ARCHITECTURE.md', slug: 'architecture', title: 'Architecture' },
  { file: 'CLIENT_FRAMEWORK_GUIDE.md', slug: 'client-framework', title: 'Client framework (Java)' },
  { file: 'ACQUISITION.md', slug: 'acquisition', title: 'Discovery, Acquisition, Invocation' },
  { file: 'UPDATE_POLICY.md', slug: 'update-policy', title: 'Update policies' },
];

export const EXAMPLES = [];
