# ADD — Configurability

Status: proposed design. All three iterations (bibliographic sources, notifications, lending policies) are planned and described at design level only. Nothing is implemented.

Requirements and scenarios: [ADD.md](ADD.md). Extensibility design this document builds on: [Extensibility.md](Extensibility.md). Lending rules: *ARQSOFT 2026-2027 Lending Policies Specification v1.0*. Organization follows the faculty material *Something about Attribute-Driven Design 0.9*, Steps 1–8. Detailed rationale is recorded in the linked technical memos.

## Step 1 — Review inputs

### Requirement

The system must let the choice of bibliographic sources and notification channels be made at setup time, without changing source code (ARQSOFT 2026-2027 Project 1 for Students 1.1, sections 1.3.3.1–1.3.3.2). The lending policy specification (sections 1 and 4) requires that the policy applied to each reader be selected automatically, according to the reader's history, on every new lending and renewal. Section 5 of that specification (external setup-time configuration of the lending parameters) is excluded for this group, following the reported faculty instruction recorded in [ADD.md](ADD.md).

### Variation and configuration points

- Bibliographic sources: which of the installed sources (Google Books, Open Library, later others) are consulted, one or several at a time. Chosen at setup.
- Notification channels: which of the installed channels (email, SMS, webhook, later others) are enabled. Chosen at setup. Each reader then chooses among the enabled channels at runtime.
- Lending policies: which installed policy (Standard, Adaptive, later others) applies to a reader. Chosen automatically at runtime, per reader and per request, from the reader's history (specification section 4).
- Lending parameter values (`MaxActiveLendings`, `LendingPeriod`, thresholds, periods, `HistoryPeriod`, `MinCompletedLendings`, `MinOnTimeRatio`, and so on): **not** externally configurable for this group. They are validated, immutable values defined in code, which keeps external configuration as an evolution point.

Retain the existing Java/Spring REST backend and the existing `config/library.properties` mechanism.

## Step 2 — Establish goals and select inputs to be considered in the iteration

### Roadmap

| Iteration | Goal | Drivers / Related requirements | Design concepts to consider |
|---|---|---|---|
| Iteration 1 | Let the Administrator choose, at setup, which bibliographic sources are consulted, one or several. | QAS4 — Configurability; US5. Related: US3, EXT-BIB-04, QAS1, QAS9 | Configuration-driven behaviour, defer binding, setup-time composition, dependency injection |
| Iteration 2 | Let the Administrator choose, at setup, which notification channels are enabled, and let each reader choose among them at runtime. | QAS5, QAS6 — Configurability; US8, US9. Related: QAS2, QAS10 | Configuration-driven behaviour, channel registry, validation against the enabled set |
| Iteration 3 | Select the applicable lending policy for each reader automatically on every new lending or renewal. | QAS8 — Configurability; US11, US14. Related: US13, QAS3, QAS11 | Configuration-driven behaviour (selection rules), dynamic binding, selector, controlled clock |

The roadmap is tentative: it orders architectural concerns, not coding stages. Design concepts are candidates to evaluate in each iteration. Each iteration closes the configuration point that the corresponding [Extensibility](Extensibility.md) iteration left open (EXT-BIB-04, NOT-MTD-01 to NOT-MTD-04, and the lending selector).

### Iteration 1 — Bibliographic information

Goal: the enabled sources come from setup configuration, not from code. Primary driver: QAS4. Related drivers: QAS1 (extension), QAS9 (test isolation). Related requirements: US5, US3.

### Iteration 2 — Reader notifications

Goal: the enabled channels come from setup configuration, and reader preferences are restricted to them. Primary drivers: QAS5 and QAS6. Related drivers: QAS2, QAS10. Related requirements: US8, US9.

### Iteration 3 — Lending policies

Goal: choose, for each reader and on every new lending or renewal, the applicable policy from the reader's history, without restart and without manual switching. Primary driver: QAS8. Related drivers: QAS3 (adding a policy), QAS11 (policy testability). Related requirements: US11, US14 and rules LP-SEL1–LP-SEL2 in [ADD.md](ADD.md).

Selection rationale: the selection rules and thresholds of section 4 are what the specification leaves variable per reader. QAS8 is the Configurability scenario in ADD.md for lending, and it states that it specifies runtime selection rather than setup-time reconfiguration. Specification section 5 (external parameter configuration) is excluded, so parameter values are not a driver of this iteration.

## Step 3 — Choose one or more elements of the system to decompose

### Elements to decompose — Iteration 1

- **Setup configuration:** the property that lists the enabled sources, read at startup.
- **Source composition:** the element that turns the configured list into the ordered collection of `BibliographicSource` implementations.
- **`BibliographicService`:** receives the collection and queries all enabled sources (US3).

### Elements to decompose — Iteration 2

- **Setup configuration:** the property that lists the enabled channels, read at startup.
- **Channel composition:** registers only the enabled `NotificationService` implementations.
- **Reader notification preferences:** validated against the enabled set at runtime.
- **Notification dispatch:** sends only through the channels both enabled and chosen by the reader.

### Elements to decompose — Iteration 3

- **Policy selector:** chooses the policy for a reader on every request (LP-SEL2).
- **Selection rules:** one rule per non-default policy, starting with Adaptive eligibility (LP-SEL1); Standard is the default (spec 4.1).
- **Reader lending history:** facts the rules need (completed lendings and on-time ratio in `HistoryPeriod`, overdue lending) without exposing repositories.
- **Time source:** an injected clock, so history windows and overdue checks are deterministic in tests.

As-is basis:

- There is no configuration for bibliographic sources or notification channels in the current code; neither area exists as a module yet (see [Extensibility.md](Extensibility.md)).
- `LendingServiceImpl.create` applies a single fixed rule set to every reader (rejects when one lending is overdue, when three are outstanding — hardcoded — and uses `lendingDurationInDays` from `config/library.properties`). There is no policy, no per-reader selection and no renewal.
- `Lending` obtains the current date with `LocalDate.now()`.

## Step 4 — Choose one or more design concepts that satisfy the inputs of the iteration

### Modifiability tactics — Supporting configurability

The image is a reference catalogue, not a selection of every tactic. Source: the faculty ADD material and the modifiability tactics.

![Modifiability tactics catalogue](Images/ModifiabilityTactics.png)

#### Selected tactics — Iterations 1 and 2

- **Defer Binding:** compose the enabled sources and channels at startup from external configuration, instead of fixing them in business logic (QAS4, QAS5). This means setup-time selection, not runtime switching or loading new code.
- **Abstract Common Services / Use of Interfaces:** the enabled implementations are used only through `BibliographicSource` and `NotificationService`, so the configuration refers to implementations by identifier and consumers never name them (QAS4, QAS5). This is the abstraction already selected in Extensibility, not an additional component.
- **Use an Intermediary:** the composition element stands between the configuration and the coordinator/dispatcher, so neither reads properties (QAS4, QAS5).
- **Separation of Concerns:** reading configuration, validating it and using it are separate responsibilities (QAS4, QAS5).
- **Restrict Dependencies:** the coordinator and dispatcher depend on the interfaces and the injected collection, not on Spring properties or the configuration file (QAS9, QAS10).

#### Selected tactics — Iteration 3

- **Use an Intermediary:** `LendingPolicySelector` stands between `LendingServiceImpl` and the policies, so the service never names a concrete policy (QAS8).
- **Separation of Concerns / Increase Semantic Coherence:** selection rules (who gets which policy) are kept apart from policy rules (what a policy allows) and from history collection (QAS8, QAS11).
- **Defer Binding:** the available policies and rules are composed at setup; the policy applied to a request is bound at runtime for that reader (LP-SEL2). This is per-request choice among installed policies, not loading new code.
- **Restrict Dependencies:** selection rules depend on a read-only history snapshot, not on repositories, HTTP or Spring (QAS11).

### Other possible tactics

The following concepts are selected as design proposals from the faculty's Tactics (iii) and (vi). They support the configuration and testing requirements; they do not imply adopting the complete categories.

#### Flexibility tactics — Tactics (iii)

- **Configuration-Driven Behavior:** enable one or several installed sources and channels through setup configuration, without changing the coordinator or dispatcher (US5, US9, QAS4, QAS5). In iteration 3 the same idea applies to the ordered list of selection rules, which is assembled at setup. It is **not** applied to the lending parameter values, because section 5 of the specification is excluded.
- **Dynamic Binding:** runtime, per-request choice of the lending policy from the reader's current history (QAS8). Reader notification preferences (QAS6) are also runtime data, restricted to the channels bound at setup.

#### Testability tactics — Tactics (vi)

- **Dependency Injection:** supply the enabled collection, the enabled-channel set and the clock from outside, so tests use any combination without a configuration file (QAS9, QAS10, QAS11).
- **Isolation:** test source/channel selection, reader preference validation and policy selection independently of HTTP, persistence and Spring.
- **Mocking:** use stubs of sources and channels to verify that only enabled ones are called, and a stub history for selection tests.

These choices are elaborated as responsibilities and decisions in Step 5.

#### Alternatives — Iterations 1 and 2

| Alternative | Description | + | − |
|---|---|---|---|
| C1 — Names read inside the coordinator/dispatcher | The coordinator reads the property and branches on provider/channel names. | Few types | Every new source or channel changes the coordinator; fails QAS1 and QAS2 |
| C2 — Identifier list bound to injected implementations | A property lists enabled identifiers; a composition element injects only the matching implementations. | Coordinator free of names and properties; testable; consistent with the extension design | A composition element and an identifier convention |
| C3 — Enable flags stored in the database | Editable at runtime through the API. | No restart | The requirement is setup-time selection; adds persistence and authorization concerns; over-engineering |

**Proposed choice:** C2. It is the only alternative that keeps new sources and channels addable without changing the coordinator or dispatcher while selecting them at setup (QAS1, QAS2, QAS4, QAS5).

#### Alternatives — Iteration 3

| Alternative | Description | + | − |
|---|---|---|---|
| L1 — Eligibility inside the service | `LendingServiceImpl` checks eligibility and branches on policy type. | Simple; few types | Every new policy or rule changes the service; fails QAS3 and QAS8 testability |
| L2 — Self-selecting policies | Each policy decides whether it applies to a reader, with a priority. | Fewer types | Mixes LP-SEL rules into policies; Standard default becomes implicit; priorities change when a policy is added |
| L3 — Separate selection rules and selector | Ordered rules map reader history to a policy; Standard is the default. | Selection tested alone; visible as its own element; supports QAS8, QAS11 | More types; rule order must be explicit |
| L4 — Selection stored or edited as external data | Selection thresholds and policy mapping kept as configuration or database data. | Changes without redeploy | Specification section 5 is excluded; over-engineering for two policies; harder to test |

**Proposed choice:** L3, the same selector design recorded in [the lending policies memo](Technical%20Memos/Extensibility/LendingPolicies.md) (EXT-LP-03). Configurability adds no new element to it; it records that selection is performed per request and that its thresholds are not externally configured.

## Step 5 — Instantiate architectural elements, allocate responsibilities, and define interfaces

### Tactics and design decisions

| ID | Design decision | Rationale |
|---|---|---|
| CFG-BIB-01 | A setup property lists the enabled bibliographic sources by identifier, in order. At startup only the matching `BibliographicSource` implementations are injected into `BibliographicService`. | Enabling Open Library, Google Books or both needs no code change and no recompilation (QAS4); the coordinator contains no provider names (EXT-BIB-04). |
| CFG-BIB-02 | An unknown source identifier stops startup with a message naming it. Behaviour for an empty list is to be confirmed. | An unknown identifier is a setup error and must not silently reduce the sources consulted. |
| CFG-NOT-01 | A setup property lists the enabled notification channels by identifier. Only the matching `NotificationService` implementations are registered. | Disabled channels send 0 notifications (QAS5); consistent with NOT-MTD-01 and NOT-MTD-02. |
| CFG-NOT-02 | Reader notification preferences are validated against the enabled set and stored per reader. A choice that includes a disabled channel is rejected with an error naming the channel. | Readers can only select enabled channels, and other readers are unaffected (QAS6, US8). |
| CFG-NOT-03 | Dispatch uses the intersection of enabled channels and the reader's chosen channels. | Later notifications follow the reader's current preference without restart (QAS6). |
| CFG-LP-01 | `LendingPolicySelector` is invoked on every new lending and every renewal request and returns the applicable policy for that reader. No selection result is cached across requests. | LP-SEL2; selection reflects the reader's current history without restart or manual switching (QAS8). |
| CFG-LP-02 | An ordered list of `PolicySelectionRule`s is assembled at setup; Standard is returned when none matches. `AdaptiveEligibilityRule` requires all three conditions: enough completed lendings, enough on-time returns, no currently overdue lending, within the history window (LP-SEL1). | Selection is data-driven by history and extensible by adding rules; Standard is an explicit default (spec 4.1). |
| CFG-LP-03 | The selection thresholds (`HistoryPeriod`, `MinCompletedLendings`, `MinOnTimeRatio`) and policy parameters are validated, immutable objects defined in code. They are not read from external configuration. | Specification section 5 is excluded for this group; keeping them in one object per policy preserves an easy path to externalize them later. |
| CFG-LP-04 | Selection only chooses a policy; it does not modify existing lendings. | Matches QAS8; renewal effects when the policy changes remain a pending faculty question. |

### Constraints

| ID | Constraint | Origin |
|---|---|---|
| CON-CFG-1 | Changing the enabled sources or channels requires no source change and no recompilation. | QAS4, QAS5 |
| CON-CFG-2 | Enabled sources and channels are read at startup; a change takes effect after one restart. | QAS4, QAS5 |
| CON-CFG-3 | Reader channel preferences are runtime data, applied without restart, and limited to enabled channels. | QAS6 |
| CON-CFG-4 | The applicable lending policy is selected per reader on every new lending and renewal. | LP-SEL2, QAS8 |
| CON-CFG-5 | Lending parameter values are not externally configured (specification section 5 excluded). Numeric constraints of sections 2–4 still apply. | ADD.md scope note |
| CON-CFG-6 | Keep the existing Java/Spring backend and the `config/library.properties` mechanism. | Project context and current code |

### Responsibility assignment rationale

| Element | Responsibility / rationale | Properties | Related drivers |
|---|---|---|---|
| Setup properties | Hold the enabled source identifiers and enabled channel identifiers; contain no logic. | Setup-time data | CON-CFG-1, CON-CFG-2 |
| Source composition | Select the matching `BibliographicSource` implementations from the configured list, in order. | Spring configuration; setup-time binding | QAS4 |
| `BibliographicService` | Consult the injected collection, without provider names or property access. | Existing coordinator (see Extensibility) | QAS4 |
| Channel composition | Register only the matching `NotificationService` implementations. | Spring configuration; setup-time binding | QAS5 |
| Reader preference validation | Compare the channels a reader chooses with the enabled set and store them. | Application rule on the reader module | QAS6 |
| Notification dispatch | Send through enabled channels the reader has chosen. | Application service | QAS5, QAS6 |
| `LendingPolicySelector` | Evaluate the ordered rules against the reader's history; default to Standard. | Intermediary; runtime per-request binding | QAS8 |
| `PolicySelectionRule` / `AdaptiveEligibilityRule` | Decide whether a reader qualifies for a policy (LP-SEL1). | Pure logic over a history snapshot | QAS8, QAS11 |
| `ReaderLendingHistory` and its provider | Provide completed, on-time and overdue facts for the window, built with an injected `Clock`. | Read-only snapshot | QAS8, QAS11 |
| `LendingServiceImpl` | Ask the selector for the policy and delegate the decision; no policy names or limits. | Existing service (changed once, see Extensibility) | QAS3, QAS8 |

Names are proposed, not existing classes.

### Connection of elements — Interface contracts

| Boundary | Input | Output / obligation |
|---|---|---|
| Setup properties → source composition | List of source identifiers | Exactly the matching implementations in order; unknown identifier fails startup |
| Setup properties → channel composition | List of channel identifiers | Exactly the matching implementations; disabled channels are not loaded and send 0 notifications |
| Reader preferences | Channels chosen by a reader | Stored if all are enabled; otherwise rejected, naming the channel; other readers unaffected |
| Selector | Reader identity and request time | Applicable policy for this request; Standard when no rule matches; no side effects on existing lendings |
| Selection rule | Read-only history snapshot | Whether the reader qualifies; no repository, clock or Spring access |

### Technical memos

[Extensibility — Bibliographic sources](Technical%20Memos/Extensibility/BibliographicSources.md)

[Extensibility — Notification methods](Technical%20Memos/Extensibility/Notifications.md)

[Extensibility — Lending policies](Technical%20Memos/Extensibility/LendingPolicies.md)


## Step 6 — Sketch views and record decisions


**Pending decisions:**

- Property names, file location and environment-variable precedence.
- Behaviour when no bibliographic source or no notification channel is enabled.
- Source of the webhook target.

## Step 7 — Perform analysis of current design and review iteration goal and design objectives

### Design Kanban

This board evaluates the configurability drivers at design level. Not addressed means no design iteration has developed the driver. Partially addressed means a structural proposal exists but contracts, impact analysis or validation remain incomplete. Addressed means the driver has a sufficiently specified and analysed design solution. No driver is marked addressed; implementation and verification are also pending.

| Not addressed | Partially addressed | Addressed in the design |
|---|---|---|
| Bibliographic source selection — QAS4: identifier list and composition proposed; property format and failure behaviour still need refinement | | |
| Notification channel selection and reader preferences — QAS5, QAS6: enabled set and validation proposed; property format, failure behaviour and preference storage still need refinement | | |

Related concerns: extension of sources, channels and policies (QAS1–QAS3) is in [Extensibility.md](Extensibility.md); test isolation (QAS9–QAS11) will be detailed under testability.

### Analysis and next verification

- **Iteration goals:** structurally supported by setup-time composition (iterations 1 and 2) and a per-request selector (iteration 3); none is verified.
- **Trade-off:** a composition element and fail-fast startup for setup choices; selection adds rule types and an explicit rule order. Changes to the enabled sources or channels need a restart, which matches the scenarios.
- **Experiments:**
    - Enable each combination of sources and channels (single, several) and confirm the effect after one restart with no code change.
    - Supply an unknown identifier and confirm startup fails with a message naming it.
    - Change a reader's channels to include a disabled one and confirm rejection naming the channel.
    - Request a lending and a renewal for readers with different histories and confirm each is evaluated with the policy its history selects.
- **Representative tests:** disabled channels send 0 notifications; other readers' preferences are unaffected; each LP-SEL1 condition and its boundaries; a reader changes policy between two requests without restart. Use Arrange → Act → Assert, with a fixed clock for selection.

## Step 8 — Refine as necessary

After resolving pending decisions and obtaining implementation and test evidence, update the memos, Kanban and roadmap. Continue with the next selected drivers; do not mark a proposed structure as a verified result.
