# Technical Memo — TST-BIB: Bibliographic testability

Status: proposed design; no production or test implementation changed.

Related iteration: [Testability — Bibliographic information](../../Testability.md).

## Issue

New bibliographic behaviour must meet the required class, domain, mutation, collaboration and system testing scopes. Implicit real collaborators or live provider dependencies obscure the SUT and reduce control.

## Problem

How can we test each relevant class separately, following the classroom isolation convention, then verify real collaboration and the running API without live external providers?

## Summary of solution

Use explicit SUT boundaries, injected behavioural dependencies and controlled fixtures. Test classes first; then real collaborating classes with local provider doubles and dedicated persistence where relevant; finally the running system through HTTP.

## Adopt the following tactics

Selected catalogue tactics: Abstract Data Sources, Limit Structural Complexity, Limit Nondeterminism, Executable Assertions and Sandbox. Dependency Injection, Isolation and Mocking put these into practice. Logging is optional diagnostic support. Interfaces, encapsulation and separation of concerns preserve the source architecture agreed in extensibility.

## Factors

- QAS9 requires reproducible source/mapping/merge tests without real external calls.
- Faculty section 1.3.3.4 distinguishes class, domain, mutation, collaboration and system scopes and prioritizes representative new behaviour.
- The student reports single-class isolation as the classroom criterion. Test names, Spring annotations and typed variables alone do not establish the SUT.
- Existing Author construction illustrates hidden real value-object collaboration, but no Author refactoring is authorized by this documentation task.

## Solution

- TST-BIB-01: record one real class and controlled behavioural collaborators for isolated class tests; declare real multi-class execution as a wider SUT.
- TST-BIB-02: inject sources and merge, and introduce a narrow application-owned transport contract for adapters. Tests replace transport/mapping collaborators as needed; broader tests exercise the real transport against local HTTP doubles. Do not mock the SUT or weaken domain rules.
- TST-BIB-03: use fixed responses, normalized data and explicit outcomes for repeatable test oracles.
- TST-BIB-04: separately test real production composition, including domain/repositories/gateways where selected, and a running-system HTTP boundary.
- TST-BIB-05: use representative AAA scenarios, meaningful assertions, coverage and mutation analysis.

Do not force DTOs or infrastructure into the domain to satisfy transparent-box testing. Identify actual affected domain classes and test their rule branches; later policy-domain work also contributes to that assignment category.

## Motivation and trade-offs

Isolation improves control and fault localization. Broader tests reveal wiring and contract errors hidden by mocks. The cost is more explicit boundaries, fixtures and double maintenance; tests can become brittle if they assert private implementation details or every incidental call. Interface additions must correspond to meaningful replaceable dependencies.

## Alternatives

- **A — Inject the HTTP client's existing contract:** fewer application types, but isolated tests can depend on client-specific APIs and call chains.
- **B — Inject a small application-owned transport contract (selected proposal):** controlled responses/errors through a narrow interface, with one extra interface/implementation to maintain. The production transport uses the selected HTTP client.

Both retain the required test categories. B is proposed to isolate adapter tests from client internals. Its exact contract/client choice needs refinement, and real transport integration still needs testing. See Step 4 of [Testability.md](../../Testability.md).

## Pending issues

- Finalize adapter/mapper/transport interfaces;
- Merge and error semantics; 
- Catalogue matching and missing-data API responses; 
- Affected domain scope; 
- Fixtures, system setup and measurement/gate definitions.

## Verification after implementation

Collect test results, stability, coverage, and mutation reports. Review surviving mutants. The 85% goal differs from QAS9’s 80% branch target. No results are confirmed yet.
