# Technical Memo — TST-BIB: Bibliographic testability

To see the implementation: [implemented contracts and verification](../../BibliographicImplementation.md). The decisions below describe the implementation; the report records contracts and evidence.

Related iteration: [Testability, iteration 1](../../Testability.md). Primary driver: QAS9; related US1/US3 and QAS4. Test categories from ProjectStatement.pdf section 1.3.3.4 constrain later verification.

## Issue

Bibliographic coordination, provider communication, mapping and merge must be testable with controlled inputs and failures. Their construction and dependencies need explicit boundaries.

## Problem

How can each bibliographic responsibility be tested independently, without live providers or hidden real collaborators?

## Summary of solution

Separate responsibilities and receive behavioural collaborators through constructors. Setup composes real implementations; isolated tests replace collaborators while keeping the SUT real.

## Adopt the following tactics

- Abstract Data Sources: source and transport contracts allow controlled responses.
- Limit Structural Complexity: separate coordination, transport, mapping and merge.
- Limit Nondeterminism: use fixed responses and explicit source order in verification.
- Dependency Injection and Isolation: expose collaborators through constructors.

Executable assertions and sandboxed collaboration tests verify the resulting structure.

## Factors

- QAS9 requires reproducible tests without live provider calls.
- Existing Author construction creates Name/Bio internally, illustrating hidden construction under the classroom SUT convention. Typed parameters alone do not guarantee isolation.
- A mapper's location follows its responsibility: API views/links belong at the REST boundary; external response mapping belongs with provider adapters.
- Bibliographic classes implement the Extensibility design; no unrelated Author refactoring was required.

## Solution

- **TST-BIB-01:** inject sources and merge into BibliographicService.
- **TST-BIB-02:** inject BibliographicTransport and the provider mapper into each adapter; HttpBibliographicTransport implements the transport contract.
- **TST-BIB-03:** use separate GoogleBooksMapper and OpenLibraryMapper classes for response transformation, independent of network and persistence.
- **TST-BIB-04:** keep BibliographicMerge independent of retrieval and registration.
- **TST-BIB-05:** keep REST representation mapping at the API boundary, and provider mapping beside its adapter.

The [class diagram](../../Images/BibliographicTestabilityClasses.png) shows the dependencies and conceptual constructors. No interface is required for every data object. No Author constructor change is part of this decision; any later domain change must preserve invariants and be justified by registration needs.

### Source lookup outcomes — Agreed behaviour

Each bibliographic source distinguishes three outcomes:

- **Found:** the source finds the book and returns the available bibliographic data, even when some fields are missing.
- **NotFound:** the source completes the lookup but finds no matching book.
- **Failure:** the source does not respond or returns an invalid response, so the lookup cannot be completed.

Missing bibliographic fields alone do not make a response invalid. These outcomes describe each source independently.

### Multiple-source lookup — Agreed behaviour

- **At least one Found:** return the available bibliographic data. If another source returns Failure, include a warning identifying the failed source and explaining that the lookup is incomplete.
- **All sources return NotFound:** report that no matching book was found in the enabled sources.
- **No Found and at least one Failure:** report that the lookup could not be completed; do not claim that all sources found no match.

An incomplete lookup means that at least one source failed, not necessarily that fields are missing. Book registration remains subject to mandatory data and valid catalogue associations. Expected merge and matching results follow the [confirmed rules in ADD.md](../../ADD.md#bibliographic-behaviour--confirmed-decisions).

## Motivation and trade-offs

Explicit collaborators make inputs and failures controllable and allow failures to be localized. The cost is more setup composition and maintained contracts. Real collaboration tests are still needed to catch incompatible implementations and incorrect wiring.

## Alternatives

- **A — Create collaborators internally:** less composition, but construction is coupled to behaviour and dependencies are harder to replace.
- **B — Receive collaborators through constructors (selected and implemented):** separate construction from behaviour and split mapping/merge responsibilities, accepting additional composition.

B is selected to support QAS9 and matches Step 4 of Testability.md. A narrow transport contract is part of this solution; Java HttpClient implements execute(URI): TransportResponse, with a configurable timeout.

## Verification

52 bibliographic tests pass; coverage is 96.10% lines and 80.25% branches for the new module. PIT killed 38/38 mutants in BibliographicMerge, SourceSelection and AuthorCorrespondence. Full regression passed 570 tests before the final 11 bibliographic cases were added. See [commands, SUT boundaries and limitations](../../BibliographicImplementation.md).
