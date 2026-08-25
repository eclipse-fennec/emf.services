import { defineConfig } from 'vitepress'
import { withMermaid } from 'vitepress-plugin-mermaid'
import { GUIDES, EXAMPLES } from '../../guides.mjs'

// Per-project docs are served under a versioned sub-path, matching the org
// convention (https://eclipse-fennec.github.io/<repo>/<version>/). The snapshot
// branch publishes to /emf.services/snapshot/; tagged releases / `latest` get added
// once the first release lands. DOCS_BRANCH is set by the reusable docs workflow
// (github.ref_name) and must match the directory the site slice is copied into.
const version = process.env.DOCS_BRANCH || 'snapshot'
const base = `/emf.services/${version}/`

// Canonical published origin. Links that point OUTSIDE the current docs base
// (other doc versions) must be full URLs — VitePress auto-prepends `base` to any
// root-absolute (`/…`) link, which would otherwise double the path.
const SITE = 'https://eclipse-fennec.github.io/emf.services'

// Version selector. Only `snapshot` is deployed today; keep as data so adding
// `latest` and tagged versions later is a one-liner.
const versions = [{ text: 'snapshot', link: `${SITE}/snapshot/` }]

const guideItems = GUIDES.map((g) => ({ text: g.title, link: `/guides/${g.slug}` }))
const exampleItems = EXAMPLES.map((g) => ({ text: g.title, link: `/examples/${g.slug}` }))

// The examples section only exists once there are examples — an empty `items`
// array renders as a dead nav entry.
const nav = [
  { text: 'Home', link: '/' },
  { text: 'User Manual', items: guideItems },
  ...(exampleItems.length ? [{ text: 'Examples', items: exampleItems }] : []),
  { text: `version: ${version}`, items: versions },
]

const sidebar: Record<string, unknown[]> = {
  '/guides/': [{ text: 'User Manual', items: guideItems }],
}
if (exampleItems.length) {
  sidebar['/examples/'] = [{ text: 'Examples', items: exampleItems }]
}

// withMermaid wraps the config so ```mermaid fences render as diagrams
// (client-side, no external assets).
export default withMermaid(defineConfig({
  title: 'Fennec Services',
  description:
    'Cross-language service registry on EMF — one Ecore service model, REST/SSE and MQTT transports, SDKs for Java and TypeScript.',
  lang: 'en-US',
  base,
  cleanUrls: true,
  lastUpdated: true,
  ignoreDeadLinks: true,

  markdown: {
    // Shiki has no dedicated 'gradle' grammar; Gradle build files are Groovy.
    languageAlias: { gradle: 'groovy' },
  },

  head: [
    ['link', { rel: 'icon', type: 'image/png', href: `${base}fennec-logo.png` }],
    ['meta', { name: 'theme-color', content: '#c0631c' }],
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:title', content: 'Fennec Services' }],
    [
      'meta',
      {
        property: 'og:description',
        content: 'Lucene as a capability-honest search backend for EMF models.',
      },
    ],
  ],

  themeConfig: {
    logo: '/fennec-logo.png',
    siteTitle: 'Fennec Services',

    nav,
    sidebar,

    socialLinks: [{ icon: 'github', link: 'https://github.com/eclipse-fennec/emf.services' }],

    search: { provider: 'local' },

    editLink: {
      pattern: 'https://github.com/eclipse-fennec/emf.services/edit/main/docs/:path',
      text: 'Edit this page on GitHub',
    },

    footer: {
      message:
        'Released under the EPL-2.0 License. Eclipse Fennec is part of the Eclipse Foundation.',
      copyright: 'Copyright © Eclipse Foundation and contributors',
    },
  },
}))
