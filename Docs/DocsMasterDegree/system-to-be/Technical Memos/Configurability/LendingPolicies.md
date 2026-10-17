# Technical Memo — CFG-LP: Lending policy configurability

Status: proposed design; not implemented or verified.

Related iteration: [Configurability — Lending policies](../../Configurability.md) (iteration 3). Primary drivers: QAS7 (parameters at setup) and QAS8 (per-reader selection at runtime). Related: QAS3, QAS11; US10, US11, US14; rules LP-SEL1–LP-SEL2 and LP-CFG1–LP-CFG2 in [ADD.md](../../ADD.md). Faculty basis: Project 1 section 1.3.3.3 (items 2 and 3) and *ARQSOFT 2026-2027 Lending Policies Specification v1.0*, sections 4 and 5.

## Issue

The current lending rules use one duration from `config/library.properties` and a limit of three lendings hardcoded in `LendingServiceImpl`. The specification requires every quantitative parameter of Standard, Adaptive and selection to be configurable at setup without source changes, invalid values to be rejected, and the applicable policy to be chosen for each reader on every new lending and renewal.

## Problem

How can all lending parameters be changed at setup, with invalid combinations detected before the system serves requests, while the policy for each reader is still chosen at runtime from their history and the policies stay free of configuration code?

## Summary of solution

Read the lending parameters from setup properties at startup and bind them into immutable, self-validating parameter objects: one per policy and one for selection. Any violated constraint stops startup with a message naming the parameter. Policies and rules receive these objects; the selector chooses the policy on every request from the reader's history.

## Adopt the following tactics

Configuration-Driven Behavior and Dynamic Binding (Tactics (iii)); Defer Binding, Use an Intermediary, Separation of Concerns and Restrict Dependencies (modifiability). Dependency Injection supports testing with any parameter combination without a configuration file.

## Factors

- Specification 5.1–5.2: all quantitative parameters configurable at setup, no source change.
- Specification 5.3: configured values must satisfy each policy's constraints (2.x positive values, 3.5 ordering, 4.2 ranges).
- Specification 4.3: selection is re-evaluated on every new lending and renewal.
- `config/library.properties` already exists and is read through Spring; the backend must be kept (CON-CFG-6).
- Policies and rules must not depend on Spring or the configuration file (CON-LP-4) so they remain testable (QAS11).

## Solution

- CFG-LP-01: the selector runs on every new lending and renewal; no selection result is cached.
- CFG-LP-02: ordered selection rules assembled at setup; Standard when none matches.
- CFG-LP-03: all parameters read from setup properties at startup into `StandardPolicyParameters`, `AdaptivePolicyParameters` and `PolicySelectionParameters`; policies and rules receive the objects and never read properties.
- CFG-LP-04: selection does not modify existing lendings.
- CFG-LP-05: each parameter object validates its constraints on construction; a violation stops startup with a message naming the parameter and the constraint.

## Motivation and trade-offs

Changing a value needs only an edit and a restart (QAS7). Validation lives with the values it protects, so it is checked once and tested without Spring. Failing at startup is safer than lending under an invalid policy. The cost is one binding element, a property naming convention for every parameter, and a restart for each change; adding a policy also adds its parameter keys.

## Alternatives

- **Values defined in code:** simplest, but every change needs a rebuild; violates specification section 5.
- **Each policy reads properties itself:** fewer types, but couples policies to Spring and the configuration file and scatters validation.
- **Parameters editable at runtime (database or admin API):** no restart, but not required by the specification, adds persistence and authorization concerns, and makes running lendings depend on changing values.
- **Validate lazily on first use:** startup always succeeds, but an invalid policy is only discovered when a reader is refused or wrongly allowed.

The selected option binds at startup into validated objects, which meets section 5 with the least coupling.

## Pending issues

- Property names, environment-variable precedence and default values.
- Whether the existing `lendingDurationInDays` key is reused for the Standard `LendingPeriod` or replaced.
- Unit of `MinOnTimeRatio` in configuration (percentage or fraction).
- Whether `fineValuePerDayInCents` (outside the specification) moves under the same binding.

## Verification

Follow Step 7 of the linked iteration: change a parameter and confirm the effect after one restart with no code change; supply each constraint violation and confirm startup fails naming it; confirm selection changes between two requests when the reader's history changes. Test design is in [Testability — Lending policies](../Testability/LendingPolicies.md). No results are claimed yet.
