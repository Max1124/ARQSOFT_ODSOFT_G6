# Technical Memo — TST-LP: Lending policy testability

Status: proposed design; no production or test implementation changed.

Related iteration: [Testability — Lending policies](../../Testability.md) (iteration 3). Primary driver: QAS11. Related: QAS3, QAS7, QAS8; US10, US11, US13, US14; rules LP-S, LP-A, LP-SEL and LP-CFG in [ADD.md](../../ADD.md). Design under test: [Extensibility — Lending policies](../Extensibility/LendingPolicies.md) and [Configurability.md](../../Configurability.md).

## Issue

Lending rules are date-dependent and currently live in `LendingServiceImpl` and `Lending`, which read `LocalDate.now()` directly. Existing lending tests start the full Spring context and compute expected values from the real date, so boundary cases cannot be reproduced and no rule can be tested on its own.

## Problem

How can each lending rule, parameter constraint and selection condition be verified with a known date and known history, first per class and then in real collaboration, without depending on the day the tests run?

## Summary of solution

Inject one `java.time.Clock`; let the history snapshot carry the reference date to policies and rules; test policies, rules, selector and parameter objects as single-class SUTs with fixtures; test the service with mocked collaborators; then verify real collaboration and startup validation against a seeded test database with test properties and a fixed clock bean.

## Adopt the following tactics

Selected catalogue tactics: Abstract Data Sources, Limit Nondeterminism, Limit Structural Complexity, Executable Assertions and Sandbox. Dependency Injection, Isolation and Mocking (Tactics (vi)) put them into practice.

## Factors

- QAS11 requires a passing and a failing case per rule, injected parameters, a controllable clock and policy tests without Spring or database.
- Assignment section 1.3.3.4 requires class, domain, mutation, collaboration and system test categories; lending contains most of the new domain rules.
- Thresholds, ratios and windows invite off-by-one errors, so boundary cases and mutation analysis matter more than test count.
- Five open faculty questions affect some expected values (CON-LP-3).
- Parameters are externally configured and validated at startup (QAS7), which needs its own tests.

## Solution

- TST-LP-01: one injected `Clock`; the snapshot carries the reference date; policies and rules never read the clock.
- TST-LP-02: policies, rules, selector and parameter objects tested as single-class SUTs with fixtures.
- TST-LP-03: `LendingServiceImpl` tested with mocked provider, selector and policy; nothing saved on rejection.
- TST-LP-04: parameter validation tested in constructors and at application startup with invalid test properties.
- TST-LP-05: collaboration and system tests use a seeded test database, test properties and a fixed clock bean.
- TST-LP-06: mutation testing on policies, rules, selector and parameter objects, with review of boundary mutants.

## Motivation and trade-offs

A single time source turns "today" into an ordinary input, so boundary dates become repeatable test cases and policies stay pure. The cost is a one-time change to how `Lending` and the service obtain the date, plus fixture builders for snapshots and seeded histories. Mocked collaborators can hide wiring errors, which is why collaboration and system tests remain required.

## Alternatives

- **Static mocking of `LocalDate.now()`:** no production change, but time stays hidden in many classes, depends on a mocking-library feature and does not help system tests.
- **Expected values relative to the real date:** no design change, but boundary dates cannot be fixed and results vary by run date — the current weakness.
- **Injected clock with the date carried by the snapshot (selected):** explicit and reproducible; requires a one-time production change.

## Pending issues

- Expected values for cases affected by the open faculty questions.
- Fixture format for seeded histories and test properties.
- Surefire/Failsafe split for context-startup tests.
- Mutation-score threshold and exclusions.

## Verification after implementation

Collect test results, coverage and mutation reports for the policy classes; confirm each LP rule has a passing and a failing case, that tests give the same results on any run date, and that invalid properties stop startup. No results are claimed yet.
