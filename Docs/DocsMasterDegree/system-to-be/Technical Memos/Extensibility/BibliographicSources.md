# Technical Memo — EXT-BIB-01: Bibliographic source extensibility

Status: proposed architecture; not implemented or verified.

Related iteration: [Extensibility, iteration 1](../../Extensibility.md). Drivers: US4/QAS1, with US1/US3, US5/QAS4 and QAS9. Faculty basis: Project 1 section 1.3.3.1 and the Technical Memo examples in *Something about Attribute-Driven Design 0.9* (PDF pages 56 and 85-87).

## Issue

The current book module has no bibliographic source abstraction. Project 1 requires Google Books, Open Library and future sources with limited impact.

## Problem

How can the backend add sources with different models and partial data without repeatedly modifying book business logic?

## Summary of solution

Introduce a common source port, an adapter per provider, normalized partial metadata and a coordinator using injected enabled sources. Keep catalogue identity resolution and persistence separate.

## Adopt the following tactics

- Abstract common services: one source contract for clients.
- Use intermediary and encapsulate change: isolate communication and external mapping behind adapters/coordinator.
- Defer binding: select installed sources at setup; configuration details remain to be refined within bibliographic iteration 1 and documented under configurability.
- Restrict dependencies and separate concerns: keep provider DTOs/HTTP outside the domain and separate retrieval, mapping, merge and persistence.

Supporting concepts selected in Step 4: Adapter and Data Transformation (interoperability), Configuration-Driven Behavior and Use of Abstractions (flexibility), and Dependency Injection, Isolation and Mocking (testability). Overlapping catalogue terms describe the same choices; they do not require extra components.

## Factors

- Multiple providers, distinct response models and missing fields are required.
- `Book` requires valid catalogue authors and genre; external names do not establish internal identity.
- Tests need controlled external responses and replaceable dependencies.
- Preserve the existing layered Java/Spring REST backend.

## Solution

EXT-BIB-01 (proposed): `BibliographicSource` defines ISBN lookup; Google/Open Library adapters normalize responses into `BibliographicData`; `BibliographicService` coordinates the selected sources; isolated merge logic combines results. Source outcomes distinguish no match from communication failure.

EXT-BIB-02 (agreed with the group): associate existing authors/genre automatically only when matching is unambiguous under defined rules. The librarian completes missing/ambiguous data and explicitly creates missing catalogue records when appropriate. Retrieval itself creates no authors/genres.

EXT-BIB-03 (proposed): isolate normalized partial metadata and deterministic merge from HTTP and persistence. Source precedence, conflicts and list handling remain pending.

EXT-BIB-04 (proposed): compose the ordered enabled source collection at setup and inject it into the coordinator. Configuration, partial-result composition and test isolation belong to roadmap iteration 1; detailed configuration remains to be refined.

## Motivation and trade-offs

New providers can implement the same contract without provider-specific branches in the business service. Test doubles can replace sources. The cost is additional types and contracts, and explicit merge/matching/failure rules. Changes to the common data needs can still affect the contract and clients.

## Alternatives

Provider-specific calls inside `BookServiceImpl` need less initial structure but require changing the service for every new source and couple business tests to external formats. The adapter/port alternative is selected as the current proposal because it addresses QAS1/QAS9 while allowing QAS4 composition.

## Pending issues

- Final API and Java contracts, catalogue matching and missing-data responses.
- Source order, conflicting/list values and explicit librarian overrides.
- Partial success and all-provider failure semantics.
- Actual extension impact and regression results after implementation.

## Verification

Follow Step 7 of the linked iteration: mapping fixtures, coordinator/merge tests, catalogue invariant checks and a third fake-source extension experiment. No passing results or effort measurements are claimed yet.
