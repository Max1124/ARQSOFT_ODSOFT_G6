# ADD — Extensibility

Status: proposed design. The current iteration addresses bibliographic sources; notifications and lending policies are planned, not yet designed or implemented.

Requirements and scenarios: [ADD.md](ADD.md). Organization follows the faculty material *Something about Attribute-Driven Design 0.9*, Steps 1–8. The detailed rationale is recorded in the linked technical memo.

## Step 1 — Review inputs

### Requirement

The system must support new bibliographic sources, notification channels and lending policies with limited impact on existing functionality (ARQSOFT 2026-2027 Project 1 for Students 1.1, sections 1.3.3.1–1.3.3.3).

### Variation and evolution points

- Bibliographic sources: Google Books and Open Library now; additional providers later.
- Notification channels: email, SMS and webhook now; additional channels later.
- Lending policies: Standard and Adaptive now; additional policies later.

Retain the existing Java/Spring REST backend. External lending parameter configuration is excluded for this group; automatic policy selection remains included pending faculty confirmation.

## Step 2 — Establish goals and select inputs to be considered in the iteration

### Roadmap

| Iteration   | Goal | Drivers / Related requirements | Design concepts to consider |
|-------------|---|---|---|
| Iteration 1 | Allow new bibliographic sources to be added with limited impact on existing book functionality. | QAS1 — Extensibility; US4. Related: US1, US3, US5, QAS4, QAS9 | Common source interface, Adapter, coordinator, normalized data, deterministic merge, setup-time binding, dependency injection |
| Iteration 2 | Support new notification channels with limited impact on existing functionality. | QAS2 — Extensibility; US6, US7. Related: US9, QAS5, QAS10 | Common channel interface, dispatcher, setup-time binding, dependency injection |
| Iteration 3 | Add lending policies with limited impact on the lending service. | QAS3 — Extensibility; US13, US14. Related: US11, QAS8, QAS11 | Policy interface, Strategy, per-reader selector, controlled clock |

The roadmap is tentative: it orders architectural concerns, not coding stages. Design concepts are candidates to evaluate in each iteration. Iterations 1 and 3 are developed below; iteration 2 is only sketched in Step 6. Later iterations can change as risks and dependencies become clearer.

Each iteration focuses on the extension point of one functional area. Configuration and testability are related concerns within that iteration, not separate bibliographic iterations. Their detailed documentation can be referenced from the corresponding attribute documents when created.

### Iteration 1 — Bibliographic information

Current iteration: Bibliographic information. Primary driver: QAS1. Related drivers: QAS4 and QAS9.

### Iteration 3 — Lending policies

Goal: add lending policies with limited impact on the lending service, while the applicable policy is chosen per reader at runtime. Primary driver: QAS3. Related drivers: QAS8 (runtime per-reader selection) and QAS11 (policy testability). Related requirements: US11, US13, US14 and rules LP-S1–LP-S5, LP-A1–LP-A5, LP-SEL1–LP-SEL2 in [ADD.md](ADD.md).

Selection rationale: lending rules are the only extension point that already exists as hardcoded business logic, so every new policy currently forces changes in `LendingServiceImpl`. QAS8 is included because selection decides which policy is applied; designing the policy contract without it would have to be revisited. External parameter configuration (faculty section 5) is excluded for this group, so configurability is not a driver of this iteration.

## Step 3 — Choose one or more elements of the system to decompose

### Elements to decompose — Iteration 1

- **Book application service:** introduce bibliographic retrieval coordination alongside existing registration logic.
- **External source integration:** separate each provider's communication and mapping behind a common source interface.
- **Bibliographic data processing:** normalize partial responses and combine results independently of HTTP and persistence.
- **Catalogue registration boundary:** associate metadata with valid internal authors/genre and preserve domain validation.

As-is basis: `BookServiceImpl.create` resolves supplied author identifiers and an existing genre before constructing `Book`. There is no bibliographic provider integration in the current book module. Initial integration may change this code; limited impact applies to subsequent source additions.

### Elements to decompose — Iteration 3

- **Lending application service (`LendingServiceImpl`):** remove policy rules from the service and delegate the decision for new lendings and renewals.
- **Lending rules:** separate each policy's rules and parameters (Standard, Adaptive) behind a common contract.
- **Policy selection:** isolate the per-reader selection rules (LP-SEL1) from the policies themselves, and evaluate them on every request (LP-SEL2).
- **Reader lending history:** provide the facts that rules need (active lendings, overdue lendings, completed and on-time lendings in a period) without exposing repositories to the rules.
- **Time source:** make "today" an explicit dependency so date-based rules can be tested deterministically (QAS11).

As-is basis:

- `LendingServiceImpl.create` (lines 59–83) iterates the reader's outstanding lendings, rejects when one is overdue (line 65) and when three are outstanding (line 70, hardcoded), then builds `Lending` with `lendingDurationInDays` injected with `@Value` (lines 32–33, value 15 in `config/library.properties`).
- `Lending` computes `startDate` and `limitDate` with `LocalDate.now()` (lines 140–141); overdue calculation also uses `LocalDate.now()` (line 183).
- There is no renewal operation and no notion of policy.

The first introduction of the policy contract changes `LendingServiceImpl` once; limited impact (QAS3) applies to subsequent policy additions.

## Step 4 — Choose one or more design concepts that satisfy the inputs of the iteration

### Design alternatives — Iteration 1

Question: where should provider communication and response mapping be placed so that adding a bibliographic source has limited impact on book business logic (QAS1)?

| Alternative | Organization | Benefits | Costs / limitations |
|---|---|---|---|
| A — Direct provider integration in the book service | Put provider HTTP calls, response mapping and provider-specific conditions inside `BookServiceImpl`. | Fewer additional types and a simpler initial implementation. | Adding or changing a provider modifies book service logic; provider formats and communication complicate its tests. |
| B — Common interface, provider adapters and a coordinator | Define `BibliographicSource`; implement one adapter per provider; let `BibliographicService` coordinate the enabled sources through that interface. | Isolates provider differences, limits changes to existing clients when adding a source, and allows controlled source doubles in tests. | Additional interfaces/classes and mapping contracts to maintain; composition and common data contracts need explicit design. |

### Selected alternative — B

Alternative B is the proposed solution because limited-impact source addition is the primary driver (QAS1). A new source implements the common contract and supplies its own communication/mapping; clients should not need new provider-specific conditions. The same boundary supports source selection at setup (QAS4) and controlled tests (QAS9). We accept the additional types and contracts as the trade-off. The existing layered backend is retained; implementation and verification are pending.

The tactics below explain how alternative B achieves these goals. They are complementary parts of that solution, not additional competing alternatives. Step 5 defines its elements and responsibilities; the technical memo records the detailed rationale.

### Modifiability tactics — Supporting extensibility

The image is a reference catalogue, not a selection of every tactic. Source: faculty ADD material — Modifiability Tactics.

![Modifiability tactics catalogue](Images/ModifiabilityTactics.png)

#### Selected tactics — Iteration 1

- **Encapsulate / Encapsulation:** hide provider-specific communication and response mapping inside each adapter, limiting the propagation of provider changes (QAS1).
- **Abstract Common Services / Use of Interfaces:** define `BibliographicSource` as a common lookup contract; new providers implement it instead of adding conditions to book business logic (QAS1). These are overlapping descriptions of the same design choice, not two required components.
- **Use an Intermediary:** let `BibliographicService` coordinate the sources through their contract rather than scatter provider calls through book registration (QAS1).
- **Restrict Dependencies:** application clients depend on the source contract; the catalogue domain does not depend on external DTOs or HTTP clients (QAS1).
- **Separation of Concerns:** separate retrieval, external mapping, merge and catalogue persistence so that changing a provider does not require changing unrelated responsibilities (QAS1, QAS9).
- **Defer Binding:** compose the enabled source implementations at setup rather than hard-code the chosen providers in business logic (QAS4). This means setup-time selection, not runtime switching or loading arbitrary new code.

### Supporting tactics

The following concepts are selected as design proposals from the faculty's Tactics (ii), (iii) and (vi). They support the related interoperability, configuration and testing requirements; they do not imply adopting the complete categories.

#### Interoperability tactics — Tactics (ii)

- **Adapter Pattern:** adapt each provider's operations and model to the common source contract (faculty section 1.3.3.1, QAS1).
- **Data Transformation:** translate external fields into partial `BibliographicData`, keeping provider differences outside the catalogue model (US3, QAS9).

#### Flexibility tactics — Tactics (iii)

- **Configuration-Driven Behavior:** enable one or several installed sources through setup configuration, without changing the coordinator (US5, QAS4).
- **Use of Abstractions:** allow provider replacement through the common source interface. This is the same abstraction already selected above, not an additional mechanism (QAS1).

#### Testability tactics — Tactics (vi)

- **Dependency Injection:** supply source implementations to the coordinator so tests can substitute controlled dependencies (QAS9).
- **Isolation:** test provider mapping and merge independently of catalogue persistence and live external systems (QAS9).
- **Mocking:** use mocks/stubs to simulate source outcomes and verify enabled-source calls; adapter tests use controlled HTTP responses where needed (QAS9).

These tactics support selected alternative B and are elaborated as responsibilities and decisions in Step 5 and in the technical memo.

#### Selected tactics — Iteration 3

Modifiability tactics (faculty ADD material, Tactics (iii), and the catalogue above):

- **Abstract Common Services / Use of Interfaces:** one `LendingPolicy` contract evaluates new lendings and renewals; Standard, Adaptive and future policies implement it (QAS3).
- **Encapsulate:** each policy hides its own parameters and rules (LP-S*, LP-A*); the service never reads `MaxActiveLendings`, thresholds or periods (QAS3).
- **Use an Intermediary:** a selector stands between the service and the policies, so the service never names a concrete policy (QAS3, QAS8).
- **Restrict Dependencies:** the service depends only on the contract and the selector; policies and selection rules depend on a read-only history snapshot, not on repositories, HTTP or Spring (QAS3, QAS11).
- **Separation of Concerns / Increase Semantic Coherence:** selection rules (who gets which policy) are separate from policy rules (what a policy allows); history collection is separate from evaluation (QAS3, QAS8, QAS11).
- **Defer Binding:** the available policies and selection rules are composed at setup; the policy applied to a request is bound at runtime for that reader (LP-SEL2). This is per-request choice among installed policies, not loading new code at runtime.

#### Other possible tactics — Iteration 3

- **Flexibility — Dynamic Binding** (Tactics (iii)): runtime, per-request policy choice from the reader's current history (QAS8).
- **Flexibility — Use of Abstractions** (Tactics (iii)): the same `LendingPolicy` abstraction already selected; not an additional mechanism.
- **Flexibility — Configuration-Driven Behavior** (Tactics (iii)): **not selected**. External parameter configuration is excluded for this group. Parameters are kept as validated, immutable objects defined in code, which keeps this as an evolution point (Step 1, *Variation vs. Evolution Points*).
- **Testability — Dependency Injection, Isolation, Mocking** (Tactics (vi)): policies receive parameters and a history snapshot, so they are tested without Spring or a database; the service is tested with a mocked selector/policy; "today" comes from an injected clock (QAS11).

Pattern: **Strategy** for policies, with an ordered list of selection rules and a default policy.

#### Alternatives — Iteration 3

| Alternative | Description | + | − |
|---|---|---|---|
| A1 — Conditional logic in the service | `LendingServiceImpl` checks eligibility and branches on policy type. | Simple; few types | Every new policy or rule changes the service; hard to test rules in isolation; fails QAS3 |
| A2 — Strategy, self-selecting policies | Each policy implements the contract and also decides if it applies to a reader (with a priority). | Fewer types; one class per policy | Mixes LP-SEL rules into policies; adding a policy may require changing others' priorities; Standard "default" becomes implicit |
| A3 — Strategy + separate selection rules + selector | Policies implement the contract; ordered selection rules map reader history to a policy; Standard is the default. | Localizes policy and selection changes; each part tested alone; supports QAS3, QAS8, QAS11 | More types; ordering of rules must be explicit |
| A4 — External rules engine / database rules | Rules described as data and interpreted at runtime. | Changes without redeploy | Over-engineering for two policies (Step 1 over-engineering alert); configuration is out of scope; harder to test |

**Proposed choice:** A3. It is the only alternative where a new policy (and, when needed, its selection rule) is added without changing the service or existing policies, and where selection and policy rules are testable independently. The cost is more types and an explicit rule order. The full rationale is recorded in the lending policy technical memo (Step 5).

## Step 5 — Instantiate architectural elements, allocate responsibilities, and define interfaces

### Tactics and design decisions

| ID | Design decision | Rationale |
|---|---|---|
| EXT-BIB-01 | Define `BibliographicSource` and one adapter per provider; inject sources into a coordinator. | New providers implement a stable contract rather than add provider-specific conditions to book business logic (QAS1). |
| EXT-BIB-02 | Automatically associate only unambiguous existing authors/genre; request librarian completion when necessary. Never create these records automatically. | Reduce manual entry while preserving catalogue identity and the agreed librarian-controlled creation of missing records (US1). |
| EXT-BIB-03 | Translate external responses to partial `BibliographicData`; separate merge logic. | Different models and missing fields remain outside the catalogue domain; merge can be tested without HTTP (US3/QAS9). |
| EXT-BIB-04 | Compose the ordered enabled source collection at setup. | One or several installed sources can be selected without embedding provider names in the coordinator (QAS4). Detailed configuration remains to be refined within iteration 1 and documented under configurability. |
|NOT-MTD-01| `NotificationService` defines general behavior of a notification mechanism; specific implementations of the interface detail the workings of specific mechanisms. | Clients depend on one notification contract rather than on specific mechanisms, so mechanisms can be added or replaced without changing the code that sends notifications (QAS2). |
|NOT-MTD-02| `SMSNotificationService`, `EmailNotificationService` and `WebhookNotificationService` implement `NotificationService` for SMS, email and webhook delivery. | Each mechanism's protocol and delivery details stay in its own class, so a change to one mechanism does not affect the others; covers the three required mechanisms (QAS2). |
|NOT-MTD-03| `LendingServiceImpl` depends only on `NotificationService`, so adding a notification mechanism does not change `LendingServiceImpl`. | Lending business logic contains no mechanism-specific calls or conditions (QAS2); it can be tested with a test double of `NotificationService` instead of real delivery (QAS10). |
|NOT-MTD-04| Notification implementations obtain the reader's contact data through `ReaderRepository`, not its Spring Data implementation: the phone number from `ReaderDetails` and the email from the `username` of its `Reader`. | Notification mechanisms do not depend on the persistence technology, so the repository can be replaced by a test double (QAS10); contact data is read from the existing reader model instead of being duplicated. |
| EXT-LP-01 | Define `LendingPolicy` as the common contract for evaluating a new lending and a renewal; it returns a `PolicyDecision` (allowed with duration, or rejected with a reason). | New policies implement a stable contract instead of adding conditions to `LendingServiceImpl` (QAS3); readers receive a reason when rejected (US13, US14). |
| EXT-LP-02 | Implement `StandardLendingPolicy` and `AdaptiveLendingPolicy`, each with an immutable parameters object validated on creation. | Each policy encapsulates its own rules and limits (LP-S1–LP-S5, LP-A1–LP-A5); invalid parameter combinations are rejected at setup (LP-A3). |
| EXT-LP-03 | `LendingPolicySelector` evaluates an ordered list of `PolicySelectionRule`s and returns the first matching policy, or Standard when none matches; it is called on every new lending and renewal. | Selection rules are kept apart from policy rules, so policies and rules change independently (QAS3); selection follows the reader's current history (LP-SEL1, LP-SEL2, QAS8). Configurability details are in CFG-LP-01 to CFG-LP-04. |
| EXT-LP-04 | Build a read-only `ReaderLendingHistory` snapshot through a provider that uses `LendingRepository` and an injected `Clock`. | Policies and rules do not depend on repositories or the system clock, so they can be tested with fixed data and dates (QAS11). |
| EXT-LP-05 | `LendingServiceImpl` obtains the history, asks the selector for the policy and applies the returned decision; it contains no policy names, limits or periods. | The service is changed once to introduce the contract; later policies do not change it (QAS3). It can be tested with a mocked selector and policy (QAS11). |

### Constraints

| ID | Constraint | Origin |
|---|---|---|
| CON-BIB-1 | Keep the existing backend and valid `Book` invariants. | Project context and current code |
| CON-BIB-2 | Support Google Books, Open Library, different models and partial data. | Faculty section 1.3.3.1 |
| CON-BIB-3 | Create missing catalogue authors/genres only by explicit librarian decision. | Group decision |
| CON-BIB-4 | Keep provider DTOs/HTTP dependencies outside the domain. | Proposed architectural boundary supporting QAS1/QAS9 |
| CON-NOT-1 | Keep the existing backend. | Project context and current code |
| CON-NOT-2 | Support email, SMS and webhook; notifications may use more than one method at a time. | ADD.md, Reader notifications requirements |
| CON-NOT-3 | Allow the notification systems to be used to be chosen at setup time. | ADD.md, Reader notifications requirements |
| CON-NOT-4 | Use the reader contact data already in the model: phone number in `ReaderDetails`, email as the `username` of `Reader`. | Current code |
| CON-NOT-5 | Keep mechanism-specific dependencies (SMS, email and webhook clients) outside `LendingServiceImpl` and the domain. | Proposed architectural boundary supporting QAS2 |
| CON-LP-1 | Keep the existing backend and valid `Lending` invariants (dates, lending number, fines). | Project context and current code |
| CON-LP-2 | Lending parameter values are not externally configured; numeric constraints of the lending rules still apply. | ADD.md scope note (specification section 5 excluded) |
| CON-LP-3 | Do not assume answers to the open faculty questions; calculations affected by them remain pending. | ADD.md, pending questions |
| CON-LP-4 | Keep policies and selection rules free of Spring, persistence and the system clock. | Proposed architectural boundary supporting QAS3/QAS11 |

### Responsibility assignment rationale

| Element | Responsibility / rationale | Properties | Related drivers           |
|---|---|---|---------------------------|
| `BibliographicSource` | Define ISBN lookup and a source identifier. | Common application port | QAS1                      |
| `GoogleBooksAdapter`, `OpenLibraryAdapter` | Query each provider and map its response. | Adapter; infrastructure boundary | QAS1, CON-BIB-2           |
| `BibliographicData` | Carry partial normalized metadata and provenance, not persisted entities. | Provider-independent data | US3, CON-BIB-4            |
| `BibliographicService` | Consult the injected enabled collection, without provider-name conditionals. | Application coordinator/intermediary | QAS1, QAS4                |
| `BibliographicMerge` | Combine normalized results without HTTP or persistence. | Isolated deterministic logic | US3, QAS9                 |
| Setup composition | Supply ordered enabled source implementations. | Spring constructor injection; detailed binding deferred | QAS4                      |
| Book registration | Resolve valid catalogue associations, validate and save. | Existing domain/repository boundaries | US1, CON-BIB-1, CON-BIB-3 |
| `NotificationService` | Define the general behaviour of a notification mechanism. | Common notification contract | QAS2 |
| `SMSNotificationService` | Deliver notifications by SMS, using the reader's phone number from `ReaderDetails`. | Implementation of `NotificationService` | QAS2, CON-NOT-2, CON-NOT-4 |
| `EmailNotificationService` | Deliver notifications by email, using the `username` of the reader's `Reader`. | Implementation of `NotificationService` | QAS2, CON-NOT-2, CON-NOT-4 |
| `WebhookNotificationService` | Deliver notifications by webhook. Source of the webhook target pending. | Implementation of `NotificationService` | QAS2, CON-NOT-2 |
| `LendingServiceImpl` | Trigger notifications through `NotificationService` only, without mechanism-specific calls. | Existing application service | QAS2, CON-NOT-1, CON-NOT-5 |
| `ReaderRepository` | Provide reader contact data to the notification implementations. | Existing repository interface | CON-NOT-4 |
| `LendingPolicy` | Evaluate a new lending or a renewal for a reader's history and return a decision. | Common policy contract (Strategy) | QAS3, US13, US14 |
| `StandardLendingPolicy`, `AdaptiveLendingPolicy` | Apply their own rules and parameters (LP-S*, LP-A*). | Policy implementations; no Spring or persistence | QAS3, CON-LP-2, CON-LP-4 |
| `StandardPolicyParameters`, `AdaptivePolicyParameters` | Hold and validate the parameter values of each policy. | Immutable; validated on creation | LP-A3, CON-LP-2 |
| `PolicyDecision` | Carry the outcome: allowed with duration, or rejected with a reason. | Result value; no persistence | US13, US14 |
| `LendingPolicySelector` | Evaluate the ordered selection rules and return the applicable policy; Standard by default. | Intermediary; runtime per-request binding | QAS3, QAS8 |
| `PolicySelectionRule`, `AdaptiveEligibilityRule` | Decide whether a reader qualifies for a policy (LP-SEL1). | Pure logic over the history snapshot | QAS8, QAS11 |
| `ReaderLendingHistory` and its provider | Provide active, overdue, completed and on-time lending facts for the reader, built with an injected `Clock`. | Read-only snapshot; provider uses `LendingRepository` | QAS11, CON-LP-4 |
| `LendingServiceImpl` (lending policies) | Obtain the history, select the policy, apply the decision and create or reject the lending. | Existing application service, changed once | QAS3, CON-LP-1 |
| Setup composition (lending) | Register the installed policies, their parameters, the ordered rules and the `Clock`. | Spring configuration; adding a policy adds a registration | QAS3, CON-LP-2 |

Names are proposed, not existing classes. Suggested placement: contract/data/coordinator in `bookmanagement.services` (optionally a bibliography subpackage), adapters/provider DTOs in `bookmanagement.infrastructure.bibliography`.

For lending policies, the suggested placement is `lendingmanagement.services.policy` for the contract, policies, parameters, decision, selector, rules and history snapshot. The history provider is an application service that uses the existing `LendingRepository`.

### Connection of elements — Interface contracts

| Boundary | Input | Output / obligation |
|---|---|---|
| Source lookup | ISBN | Normalized partial data or explicit no-match/failure outcome; no catalogue writes |
| Coordinator | ISBN and injected sources | Combined data and source outcomes; no automatic author/genre creation |
| Merge | Ordered normalized successful results | Combined data/provenance; precedence/conflict rules pending |
| Registration | Complete required metadata and resolved catalogue associations | Valid saved `Book`, or clear validation response without saving an incomplete book |
| Notification Mehtod | Notification methods a Reader has elected to use | Reader must have selected at least one notification method and have valid contact info petaining to it |
| Lending history | Reader number and current date from the injected `Clock` | Read-only `ReaderLendingHistory`; no writes |
| Policy selection | `ReaderLendingHistory` | Applicable `LendingPolicy`; Standard when no rule matches; no changes to existing lendings |
| Policy — new lending | `ReaderLendingHistory` | `PolicyDecision`: allowed with duration in days, or rejected with a reason; no persistence |
| Policy — renewal | Lending to renew and `ReaderLendingHistory` | `PolicyDecision`: allowed with extension, or rejected with a reason; Adaptive renewal calculation pending faculty answer |
| Lending service | New lending or renewal request | Saved `Lending` with its limit date taken from the decision, or a rejection carrying the decision reason |
### Technical Memo

[Extensibility — Bibliographic sources](Technical%20Memos/Extensibility/BibliographicSources.md)

[Extensibility — Notification Methods](Technical%20Memos/Extensibility/Notifications.md)

[Extensibility — Lending policies](Technical%20Memos/Extensibility/LendingPolicies.md)

## Step 6 — Sketch views and record decisions

### Sequence sketch — Retrieve bibliographic information

![Bibliographic retrieval sequence](Images/BibliographicRetrievalSequence.png)

### Structural sketch — Source extension point

![Bibliographic source structure](Images/BibliographicSourceStructure.png)

**Pending decisions:** matching rules and REST responses; no-match versus provider failure; partial/all-provider failure behaviour; source precedence and conflicting/list values; librarian overrides. Provider schemas/endpoints must be checked against official documentation when implementing. Full ISBN-only completion cannot be guaranteed when required catalogue data is missing.

### Sequence sketch — Notification methods information

![Notification methods sequence](Images/NotificationSequence.png)

### Structural sketch — Notification methods

![Notification methods structure](Images/NotificationStructure.png)

**Pending decisions:** more notification methods to add; notification method failure behavior.

## Step 7 — Perform analysis of current design and review iteration goal and design objectives

### Design Kanban

This board evaluates the extensibility drivers at design level. Not addressed means no design iteration has developed the driver. Partially addressed means a structural proposal exists but contracts, impact analysis or validation remain incomplete. Addressed means the driver has a sufficiently specified and analysed design solution, not merely that a constraint has been agreed. No extensibility driver is marked addressed yet; implementation and verification are also pending.

| Not addressed | Partially addressed | Addressed in the design |
|---|---|---|
|  | Notification extensibility — QAS2, Bibliographic source extensibility — QAS1: common interface/adapters proposed; contract, registration mechanism and extension impact still need refinement/validation |  |
| Lending policy extensibility — QAS3 | | |

The reported final coverage goal is at least 85%; metric and measured scope still need confirmation. Coverage alone does not verify extensibility or test quality.

## Step 8 — Refine as necessary

After resolving pending decisions and obtaining implementation/test evidence, update the memo, diagrams, Kanban and roadmap. Continue with the next selected drivers; do not mark a proposed structure as a verified result.
