# Technical Memo — TST-LP: Lending policy testability

Status: proposed design; no production or test implementation changed.

Related iteration: [Testability — Lending policies](../../Testability.md) (iteration 3). Primary driver: QAS11. Related: QAS3, QAS7, QAS8; US10, US11, US13, US14; rules LP-S, LP-A, LP-SEL and LP-CFG in [ADD.md](../../ADD.md). Design under test: [Extensibility — Lending policies](../Extensibility/LendingPolicies.md) and [Configurability.md](../../Configurability.md).

## Issue

The lending rules cannot be controlled or observed at class level in the current structure. "Today" is read from `LocalDate.now()` inside `Lending`, `LendingNumber`, `SpringDataLendingRepository` and `LendingServiceImpl`; the overdue rule and the limit of 3 sit inside `LendingServiceImpl.create` next to repository access; configuration enters through `@Value` fields; and the outcome of a rule is only an exception message. The only service and repository lending tests are `@SpringBootTest` tests with real repositories, so the lending package has integration tests and no class-level test of the service or of any rule.

## Problem

Which architectural changes make each lending rule, parameter constraint and selection condition controllable (known date, parameters, history) and observable (structured outcome) without Spring or a database, and in what order must tests be written so that integration tests never stand in for missing class tests?

## Summary of solution

Make time and configuration explicit inputs (one injected `Clock`, immutable parameter objects); extract the rules into pure policies over a read-only history snapshot that return a structured decision; reduce `LendingServiceImpl` to orchestration with constructor-injected collaborators. Fix the testing order bottom-up: class-level tests of every element first, then mutation analysis, then real collaboration, then system. The test boundaries follow from these changes; writing the tests of section 1.3.3.4 is implementation work.

## Adopt the following tactics

Selected catalogue tactics: Abstract Data Sources, Limit Nondeterminism, Limit Structural Complexity, Executable Assertions and Sandbox. Dependency Injection, Isolation and Mocking (Tactics (vi)) put them into practice. Bottom-up test order follows the unit-versus-integration distinction of the software testing material.

## Factors

- QAS11 requires a passing and a failing case per rule, injected parameters, a controllable clock and policy tests without Spring or database.
- Assignment section 1.3.3.4 requires class, domain, mutation, collaboration and system test categories; lending contains most of the new domain rules.
- Thresholds, ratios and windows invite off-by-one errors, so boundary cases and mutation analysis matter more than test count.
- The current lending tests are integration tests only; an integration test cannot attribute a failure to a rule, to wiring or to persistence.
- Behaviour the specification leaves open (Adaptive count, renewal, history window) is decided in the design and stated in the tests that rely on it (CON-LP-3).
- Parameters are externally configured and validated at startup (QAS7).
- Every seam must also serve the production design (CON-TST-11); no test-only hooks.

## Solution

- TST-LP-01: time is an injected input (one `Clock`); `Lending`, `LendingNumber` and the repository date queries receive the reference date; the snapshot carries it to policies and rules.
- TST-LP-02: configuration is an explicit input (immutable parameter objects, constructor validation); `@Value` and `@PropertySource` only at the binding edge.
- TST-LP-03: rules extracted from `LendingServiceImpl` into pure policies returning a structured `PolicyDecision`.
- TST-LP-04: reader facts provided by a read-only history snapshot built behind a narrow port.
- TST-LP-05: `LendingServiceImpl` reduced to orchestration with constructor-injected collaborators.
- TST-LP-06: fail-fast startup validation of parameters.
- TST-LP-07: bottom-up test order and unit-before-integration gate (CON-TST-10): class tests, then collaboration, then system; unit and integration suites run as separate stages.
- TST-LP-08: collaboration and system tests use a seeded test database, test properties and a fixed clock bean.
- TST-LP-09: mutation testing on policies, rules, selector and parameter objects, with review of boundary mutants.

## Motivation and trade-offs

A single time source and explicit parameters turn "today" and configuration into ordinary inputs, so boundary cases are repeatable; extracted pure policies make each rule a single-class SUT and the structured decision makes outcomes observable. The cost is a one-time change to `Lending`, `LendingNumber`, the repository date queries and the service, plus fixture builders for snapshots and seeded histories. Mocked collaborators can hide wiring errors, which is why collaboration and system tests remain required, but only after the class tests they rely on.

## Alternatives

- **A, tests only (no production change):** Spring-context tests, static time mocking and reflection to set `@Value` fields. Rules stay unreachable alone, so QAS11 cannot be met and failures cannot be localised.
- **B, seams on existing classes only:** inject a `Clock` and parameters but keep the rules in `LendingServiceImpl`. Time and values become controllable, but a rule is testable only through the whole service, observability is unchanged and adding a policy still changes the service.
- **C, seams plus extracted policies, selector, history port and parameter objects (selected):** every rule is a single-class SUT; the service is a thin orchestrator; one design serves QAS3, QAS7, QAS8 and QAS11.

Time source: static mocking and expected values computed from the real date are rejected; an injected `Clock` with the date carried by the snapshot is selected.

## Pending issues

- Fixture format for seeded histories and test properties.
- Surefire/Failsafe split so the unit stage gates the integration stage.
- Mutation-score threshold and exclusions.

## Verification after implementation

Collect test results, coverage and mutation reports for the policy classes; confirm each LP rule has a passing and a failing case, that class-level tests exist and pass for every class included in a collaboration test, that tests give the same results on any run date, and that invalid properties stop startup. No results are claimed yet.
