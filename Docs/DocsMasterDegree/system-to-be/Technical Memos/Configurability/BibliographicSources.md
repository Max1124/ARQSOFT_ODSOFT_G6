# Technical Memo — CFG-BIB: Bibliographic configurability

To see the implementation: [implemented contracts and verification](../../BibliographicImplementation.md). The decisions below describe the implementation; the report records contracts and evidence.

Related iteration: [Configurability — Iteration 1](../../Configurability.md). Primary driver: US5/QAS4; related QAS1/QAS9 and EXT-BIB-04. Faculty basis: Project 1 section 1.3.3.1. Related designs: [Extensibility](../../Extensibility.md) and [Testability](../../Testability.md).

## Issue

Project 1 section 1.3.3.1 requires selection of one or several bibliographic sources at setup. US5 and QAS4 express this configuration point; EXT-BIB-04 leaves its composition to be refined here.

## Problem

How can an Administrator select installed providers without changing or rebuilding the application, while keeping provider names outside the bibliographic coordinator?

## Summary of solution

Read an external ordered identifier list at startup, validate it against installed sources and inject the selected collection into `BibliographicService`. Changes take effect after restarting the same application artifact.

## Adopt the following tactics

- **Defer Binding:** choose installed sources at startup.
- **Use an Intermediary:** composition resolves configuration outside the service.
- **Restrict Dependencies / Separation of Concerns:** the coordinator uses the source interface; property parsing and validation stay outside it.
- **Configuration-Driven Behavior and Dependency Injection:** external data determines the injected collection; tests can supply controlled sources.

## Factors

- Faculty section 1.3.3.1 requires Google Books, Open Library and setup selection of one or several sources.
- US5/QAS4 require source selection without source-code changes or recompilation, with configuration changes taking effect after an application restart.
- QAS1 requires limited impact when adding providers; QAS9 requires controlled tests.
- Existing classpath `config/library.properties` does not alone demonstrate configuration external to the application artifact.

## Solution

- **CFG-BIB-01:** use `bibliographic.sources=google-books,open-library`, externally supplied at deployment. Build a registry from injected registrations pairing stable identifiers with installed sources; resolve the configured identifiers and inject the selected sources. A new provider adds its adapter and registration without changing the generic resolution algorithm.
- **CFG-BIB-02:** reject missing/empty lists, unknown or repeated identifiers and duplicate registered identifiers. Trim whitespace; identifiers are case-sensitive. These rules are implemented.
- **CFG-BIB-03:** preserve configured order in an immutable collection for the running instance. Apply changes after restart; never use provider-name conditionals in the service.

Selection validation performs no live provider requests. Disabled sources receive no lookup calls. Preserve source order to apply the [merge priority defined in ADD.md](../../ADD.md#bibliographic-behaviour--confirmed-decisions). Adding a new source implementation requires development/deployment; configuration chooses already installed sources.

## Motivation and trade-offs

The choice meets setup selection while reusing the extensibility interface and testability boundaries. It costs a small registry/composition mechanism and a stable identifier convention. Invalid setup prevents startup instead of silently consulting unintended sources. Restarting is acceptable under QAS4.

## Alternatives

- **A — Read names inside the coordinator:** initially simpler, but ties coordination to properties and provider identifiers.
- **B — Resolve identifiers at startup and inject the selected collection (selected):** isolates selection and makes configuration independently testable.
- **C — Database selection with an administration API:** allows runtime changes but adds persistence, authorization and reload behaviour beyond the current setup-time requirement.

## Verification

52 bibliographic tests pass; coverage is 96.10% lines and 80.25% branches for the new module. PIT killed 38/38 mutants in BibliographicMerge, SourceSelection and AuthorCorrespondence. Full regression passed 570 tests before the final 11 bibliographic cases were added. See [commands, SUT boundaries and limitations](../../BibliographicImplementation.md).
