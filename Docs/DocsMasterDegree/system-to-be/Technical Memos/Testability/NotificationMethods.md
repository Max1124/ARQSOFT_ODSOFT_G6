# Technical Memo: Notification testability

Status: proposed design; no production or test implementation changed.

Related iteration: [Testability — Notification methods](../../Testability.md).
## Issue

New notification behaviour must meet the required class, domain, mutation, collaboration and system testing scopes. Implicit real collaborators or external API dependencies obscure the SUT and reduce control.

## Problem

How can we test each relevant class separately, following the responsibility isolation convention, then verify real collaboration and the running API without actually sending notifications?

## Adopt the following tactics

Selected catalogue tactics: Abstract Data Sources, Limit Structural Complexity, Limit Nondeterminism, Executable Assertions and Sandbox. Dependency Injection, Isolation and Mocking put these into practice. Logging is optional diagnostic support. Interfaces, encapsulation and separation of concerns preserve the source architecture agreed in extensibility.

## Tactics

- Config files: the available methods will be defined in a project-level configuration.
- Encapsulate change: isolate specific mechanisms in specific instantiations.
- Defer binding: select notification methods at setup; configuration details remain to be refined within bibliographic iteration 1 and documented under configurability.
- Restrict dependencies and separate concerns: keep the implementation and calling of responsibility restricted only to relevant classes.

Supporting concepts selected in Step 4: Data Transformation (interoperability), Configuration-Driven Behavior , Isolation and Mocking (testability).

## Factors

- QAS10 requires reproducible tests without real notification calls.
- Faculty section 1.3.3.4 distinguishes class, domain, mutation, collaboration and system scopes and prioritizes representative new behaviour.
- The student reports single-class isolation as the classroom criterion. Test names, Spring annotations and typed variables alone do not establish the SUT.
- Existing Author construction illustrates hidden real value-object collaboration, but no Author refactoring is authorized by this documentation task.



## Solution

TST-NOT-01: Consumers of notifications depend only on `NotificationService`; tests inject a recording double that stores reader, channel and content. 

TST-NOT-02: Give each channel implementation an injectable delivery boundary (mail, SMS or HTTP client) and its `ReaderRepository`; test each as a single-class SUT with those replaced. 

TST-NOT-03: Test channel composition with test properties: only listed identifiers are registered; empty or unknown identifiers behave as agreed. 

TST-NOT-04: Test preference validation and dispatch as separate class SUTs: a choice including a disabled channel is rejected naming it; dispatch uses the intersection of enabled and chosen channels. 

TST-NOT-05: Run collaboration and system tests with test properties, a test database with known readers and preferences, and recording or local delivery doubles. 

TST-NOT-06: Apply mutation testing to channel implementations, preference validation and dispatch; review surviving mutants on the intersection and on the enabled-set check. 


## Motivation and trade-offs

Isolation improves control and fault localization. Broader tests reveal wiring and contract errors hidden by mocks. The cost is more explicit boundaries, fixtures and double maintenance; tests can become brittle if they assert private implementation details or every incidental call. Interface additions must correspond to meaningful replaceable dependencies.

## Alternatives

- **A — Inject the HTTP client's existing contract:** fewer application types, but isolated tests can depend on client-specific APIs and call chains.
- **B — Inject a small application-owned transport contract (selected proposal):** controlled responses/errors through a narrow interface, with one extra interface/implementation to maintain. The production transport uses the selected HTTP client.

Both retain the required test categories. B is proposed to isolate adapter tests from client internals. Its exact contract/client choice needs refinement, and real transport integration still needs testing. See Step 4 of [Testability.md](../../Testability.md).

## Pending issues

- Finalize adapter/mapper/transport interfaces;
- Merge and error semantics;
- Notification method matching and missing-data API responses;
- Affected domain scope;
- Fixtures, system setup and measurement/gate definitions.

## Verification after implementation

Collect test results, stability, coverage, and mutation reports. Review surviving mutants. The 85% goal differs from QAS9’s 80% branch target. No results are confirmed yet.
