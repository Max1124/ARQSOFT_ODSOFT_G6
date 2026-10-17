# Technical Memo — EXT-LP: Lending policy extensibility

Status: proposed architecture; not implemented or verified.

Related iteration: [Extensibility, iteration 3](../../Extensibility.md). Primary driver: QAS3. Related drivers: QAS8, QAS11. Related requirements: US11, US13, US14 and lending rules LP-S1–LP-S5, LP-A1–LP-A5, LP-SEL1–LP-SEL2 in [ADD.md](../../ADD.md). Faculty basis: Project 1 section 1.3.3.3, *ARQSOFT 2026-2027 Lending Policies Specification v1.0* (sections 2–4; section 5 excluded for this group) and the *Technical Memo* examples in *Something about Attribute-Driven Design 0.9* (Step 5).

## Issue

Lending rules are hardcoded in `LendingServiceImpl.create`: a fixed limit of three outstanding lendings, a rejection when any lending is overdue, and a single lending duration read from `library.properties`. There is no notion of policy, no renewal and no per-reader selection. Project 1 requires Standard and Adaptive policies, automatic selection for each reader, and new policies with limited impact.

## Problem

How can lending rules vary by policy, with the applicable policy chosen for each reader on every new lending or renewal, so that new policies are added without changing the lending service or the existing policies?

## Summary of solution

Adopt the following tactics: Abstract Common Services, Encapsulate, Use an Intermediary, Restrict Dependencies and Defer Binding. Apply the Strategy pattern to policies, keep selection rules separate from policy rules, and give both a read-only snapshot of the reader's lending history built with an injected clock.

## Factors

- Two policies are required now (Standard, Adaptive); more are expected (evolution point).
- Selection depends on the reader's history (`HistoryPeriod`, `MinCompletedLendings`, `MinOnTimeRatio`, no overdue lending) and must be re-evaluated on every new lending and renewal (LP-SEL1, LP-SEL2).
- Rules depend on the current date (overdue lendings, history window, due dates), so tests need a controllable time source (QAS11).
- External parameter configuration (faculty section 5) is excluded for this group. Parameters still have validity constraints (positive values, LP-A3 ordering).
- Five open faculty questions affect calculations: Adaptive count before or after the requested lending; Adaptive renewal duration and limit; interaction when the selected policy changes on renewal; history window dates and boundaries; whether an overdue lending can be renewed under Adaptive.
- Preserve the existing layered Java/Spring REST backend and the `Lending` invariants.

## Solution

EXT-LP-01 (proposed): `LendingPolicy` is the common contract. It evaluates a new lending and a renewal and returns a `PolicyDecision`: allowed with the resulting duration, or rejected with a reason that can be returned to the reader (US13, US14).

EXT-LP-02 (proposed): `StandardLendingPolicy` and `AdaptiveLendingPolicy` implement the contract. Each receives an immutable parameters object (`StandardPolicyParameters`, `AdaptivePolicyParameters`) that validates its constraints when created (LP-S1–LP-S3, LP-A3). Parameter values are defined in code because external configuration is excluded.

EXT-LP-03 (proposed): `LendingPolicySelector` evaluates an ordered list of `PolicySelectionRule`s against the reader's history and returns the first matching policy, or Standard when none matches. `AdaptiveEligibilityRule` implements LP-SEL1. The selector is called on every new lending and renewal (LP-SEL2).

EXT-LP-04 (proposed): `ReaderLendingHistory` is a read-only snapshot with the facts rules need: active lendings, overdue lendings, and completed and on-time lendings in the history window. A provider builds it from `LendingRepository` using an injected `Clock`, so rules never access repositories or the system clock directly.

EXT-LP-05 (proposed): `LendingServiceImpl` builds the history, asks the selector for the policy, asks the policy for a decision, and then creates or rejects the lending. It contains no policy names, limits or periods.

## Motivation and trade-offs

A new policy is a new class plus its parameters and tests. When it needs its own selection condition, it adds a new rule and registers both at setup. The service, the existing policies and the existing rules stay unchanged, which is the QAS3 response measure. Policies and rules receive plain data, so they can be tested without Spring or a database (QAS11), and selection is visible as its own element (QAS8).

The cost is more types (contract, two policies, two parameter objects, selector, rules, history snapshot and provider) and an explicit rule order that must be documented. The first introduction changes `LendingServiceImpl` once. If a future policy needs a fact that the history snapshot does not provide, the snapshot and its provider must change.

## Alternatives

- **Conditional logic in `LendingServiceImpl`:** simplest now, but every new policy or eligibility rule changes the service, and rules cannot be tested apart from it. Fails QAS3.
- **Self-selecting policies:** each policy also decides whether it applies, with a priority. It needs fewer types, but mixes LP-SEL rules into policy classes, makes the Standard default implicit, and adding a policy may require changing other priorities.
- **External rules engine or database-defined rules:** allows changes without redeploying, but is over-engineering for two policies, depends on configuration that is out of scope, and is harder to test.

The selected alternative separates what a policy allows from who gets it. It is the only option among these that meets QAS3 while keeping QAS8 and QAS11 testable in isolation.

## Pending issues

- Answers to the five open faculty questions in [ADD.md](../../ADD.md); the contract does not depend on them, but the Adaptive and renewal calculations do.
- Renewal model on `Lending`: renewal count, and whether to record the policy applied when the lending was created.
- Rule order when more than one non-default rule matches.
- REST response format for rejection reasons.
- Where parameter values are defined (constants or setup composition).
- Concurrent requests for the same reader can both pass the active-lending limit; this already exists in the current code and is not solved by this design.
- Optional US12 endpoint to consult the applicable policy.

## Verification

Follow Step 7 of the linked iteration:

- One passing and one failing test per LP rule, with a fixed clock and no Spring or database.
- Selector tests on each LP-SEL1 condition and its boundaries.
- `LendingServiceImpl` tests with a mocked selector and policy.
- Extension experiment: add a fake third policy and rule, and record which files change (expected: the new classes, setup registration and new tests only).

No passing results or effort measurements are claimed yet.
