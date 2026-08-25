---
layout: home

hero:
  name: Fennec Services
  text: Cross-language service registry on EMF
  tagline: A dynamic distributed service registry (working title DDSR) — language-independent OSGi-style service model, REST/SSE and MQTT transports, SDKs for Java and TypeScript.
  image:
    src: /fennec-logo.png
    alt: Eclipse Fennec logo
  actions:
    - theme: brand
      text: Getting started
      link: /guides/getting-started
    - theme: alt
      text: Vision & Requirements
      link: /guides/requirements
    - theme: alt
      text: Architecture
      link: /guides/architecture
    - theme: alt
      text: View on GitHub
      link: https://github.com/eclipse-fennec/emf.services

features:
  - icon: 🌐
    title: Cross-language by model
    details: One EMF/Ecore service model drives Java and TypeScript alike — providers and consumers on both sides talk to the same broker over a shared XMI wire format.
    link: /guides/architecture
    linkText: How it works
  - icon: 🔁
    title: Lifecycle done like OSGi
    details: Consumers are informed before a service disappears — producer shutdown blocks until the broker confirmed the withdrawal and fanned the event out.
    link: /guides/acquisition
    linkText: The three stages
  - icon: 🔏
    title: Content-addressed contracts
    details: sd1 fingerprints make service descriptions comparable across producer, broker and consumer with a string comparison — identical in both languages, pinned by a shared golden test.
    link: /guides/architecture
    linkText: Wire format
---
