# ADD — Testability

Requirements and scenarios: ADD.md. Organization follows the faculty material Something about Attribute-Driven Design 0.9, Steps 1–8. The detailed rationale is recorded in the linked technical memos.

## Step 1 — Review inputs

### Requirement

ProjectStatement.pdf, section 1.3.3.4, requires:

- Functional opaque-box tests with SUT = classes;
- Functional transparent-box tests with SUT = domain classes;
- Mutation tests with SUT = classes;
- Opaque-box tests with SUT = controller + service + {domain, repository, gateways};
- Opaque-box tests with SUT = system.

Tests should primarily cover new functionality and prioritize quality and representativeness.

For bibliographic information, QAS9 requires controlled source responses, isolated mapping/merge tests and no real external calls. US1/US3 concern ISBN-assisted registration and combining partial information; US5/QAS4 require selecting sources at setup.

For notification methods, QAS10 requires channels to be replaced by test doubles that record the calls, so that tests assert which channels were used, for which reader and with what content, with 0 real messages sent and at least 80% branch coverage on the dispatcher and channel selection logic. US6/US7 define the due-date and overdue/fine events, US8 the reader's choice of several channels and US9 the setup-time selection of enabled channels; rules EXT-NOT-01 to EXT-NOT-04 in [Extensibility.md](Extensibility.md) and CFG-NOT-01 to CFG-NOT-03 in [Configurability.md](Configurability.md) define the expected behaviour. US8 remains proposed scope.

For lending policies, QAS11 requires each policy rule, the parameter constraints, renewals and per-reader selection to be verified with injected parameters and a controllable clock, without starting Spring or the database for class tests. US10/QAS7 add startup validation of configured parameters; US11/US13/US14 and rules LP-S, LP-A, LP-SEL and LP-CFG in [ADD.md](ADD.md) define the expected behaviour.

### Current limitations

- `BookServiceImpl.create` resolves authors/genre, constructs a real `Book` and saves it through the repository. No `BookServiceImplTest` was found in the current test tree. Future tests must explicitly define which collaborators are real and which are replaced before claiming a single-class SUT.
- The bibliographic contract, coordinator, adapters and merge are design proposals, not implemented classes in the current source tree.
- Current `AuthorTest` constructs `Author`, which creates `Name`, `Bio` and photo-related objects internally. Under the single-class SUT criterion reported from class, these real collaborations must be distinguished from isolated class tests. A test of `EntityWithPhoto` is also located in that test class.
- Notifications: the current code has no notification module, so there is nothing to test yet and no existing test to preserve. `NotificationService`, its three implementations, the channel composition, the dispatch rule and the reader preference storage are design proposals, not implemented classes. Real delivery (SMTP, SMS gateway, webhook receiver) is outside the control of the tests, and `LendingServiceImpl` does not yet depend on a notification contract. How a due-date or overdue event is detected and triggered, the webhook target and the failure behaviour of a channel are still undefined, so their tests cannot yet have expected values.
- Some existing tests start Spring and replace repositories with mocks. An annotation or test name does not determine the SUT. Their actual real collaborators and assertions must be inspected.
- Lending: `LendingTest` reads `config/library.properties` through `@PropertySource` and asserts against `LocalDate.now()`; `LendingServiceImplTest` and `LendingRepositoryIntegrationTest` use `@SpringBootTest` and real-date values. Date-dependent results therefore change with the day the tests run, and no lending rule can be tested without the full context. The policy contract, selector, history snapshot and parameter objects are design proposals, not implemented classes.

### SUT convention for this project

For a class-isolation test, identify one real production class as SUT and control its behavioural collaborators. Test collaborating production classes individually before verifying their real collaboration. If a helper/mapper/transport/domain class executes real behaviour relevant to the test, declare the broader boundary and classify it as collaboration/integration rather than claim single-class isolation.

Plain input/output fixtures and standard library/framework execution must be distinguished from application collaborators; document the boundary rather than claiming that only one object can exist. A typed variable (`Author author`) or typed constructor parameters alone do not prove isolation. Receiving already prepared `Name`/`Bio` can expose dependencies, but the actual executed behaviour and constructor invariants still matter. Do not mock the SUT or weaken invariants just to label a test unitary.

## Step 2 — Establish goals and select inputs to be considered in the iteration

### Roadmap

| Iteration | Goal | Drivers / Related requirements | Design concepts to consider |
|---|---|---|---|
| Iteration 1 | Make bibliographic classes and their collaboration testable with controlled data, without live provider dependencies. | QAS9 — Testability; US1, US3. Related: US5/QAS4 and section 1.3.3.4 | Explicit SUT boundaries, dependency injection, isolated mapping/merge, HTTP doubles, AAA, mutation analysis |
| Iteration 2 | Verify notification dispatch and channels without sending real messages. | QAS10 — Testability; US6, US7, US9; US8 remains proposed scope | Channel doubles, recording interactions, transport boundaries |
| Iteration 3 | Verify policies, parameter validation and per-reader selection with controlled dates and history. | QAS11 — Testability; US10, US11, US13, US14. Related: QAS3, QAS7, QAS8 | Policy isolation, controlled clock, history fixtures, boundary cases, startup validation tests |

The roadmap groups testing-related architectural concerns, not test counts. Lending parameters are externally configured (QAS7), so parameter validation is tested in isolation; automatic policy selection remains included pending faculty confirmation.

### Iteration 1 — Bibliographic information

Primary driver: QAS9. Goal: specify observable contracts and replaceable dependencies so class tests can precede tests of real collaboration, followed by system tests. Preserve the source interface and catalogue rules agreed in extensibility.

### Iteration 2 — Notification methods

Primary driver: QAS10. Related drivers: QAS2 (adding a channel), QAS5 (setup-time channel selection), QAS6 (reader preferences). Goal: make it possible to verify that a reader is notified through exactly the channels that are enabled and chosen, with the right content, without sending any real message. Class tests of each channel and of the dispatch rule come first, then real collaboration with the lending workflow, then the running system. Preserve the `NotificationService` contract and `ReaderRepository` access agreed in [Extensibility.md](Extensibility.md) and the enabled-set, preference and intersection rules agreed in [Configurability.md](Configurability.md).

Selection rationale: the tests must show that a reader receives a due-date or overdue notification through every channel that is both enabled and chosen, and through no other, with the correct recipient and content. They must also show that a choice including a disabled channel is rejected and that one reader's preference does not affect another's. All of this is checked by recording the calls made to replaced channels, so no real message is sent.

### Iteration 3 — Lending policies

Primary driver: QAS11. Related drivers: QAS3 (adding a policy), QAS7 (parameter configuration), QAS8 (per-reader selection). Goal: make every lending rule, parameter constraint and selection condition verifiable with fixed dates, prepared histories and explicit parameter values, first per class and then in real collaboration. Preserve the policy contract, selector and history snapshot agreed in [Extensibility.md](Extensibility.md) and the parameter binding in [Configurability.md](Configurability.md).

Selection rationale: lending rules are date-dependent and currently only testable with the full Spring context and the real clock. They also contain most of the new domain logic of this project, so they are the main source of transparent-box and mutation evidence (section 1.3.3.4).

## Step 3 — Choose one or more elements of the system to decompose

### Elements to analyse — Iteration 1

- **Bibliographic coordinator:** isolate source selection/coordination from real providers and merge behaviour.
- **Provider adapters and mapping:** expose HTTP/response mapping dependencies to test each production class separately before combining them.
- **Merge logic:** separate data combination from HTTP, persistence and framework startup.
- **Catalogue registration:** identify which class enforces each rule and where real author/genre/domain interactions enter the test boundary.
- **Book REST workflow:** define a larger integration SUT and a running-system SUT after class behaviour is covered.

This refines the design in Extensibility.md; it is not a refactoring of all existing modules or a requirement to create interfaces for every data class.

### Elements to analyse — Iteration 2

- **Channel implementations (`SMSNotificationService`, `EmailNotificationService`, `WebhookNotificationService`):** build the message from the event and contact data and hand it to the delivery mechanism; the main class and mutation SUTs. Their delivery boundary must be replaceable.
- **Channel composition:** registers only the `NotificationService` implementations listed in the setup property; tested with valid, empty and unknown identifiers.
- **Dispatch (intersection of enabled and chosen channels):** selects the channels used for a reader; boundary cases are an empty intersection, one channel and several channels. Where this logic lives is pending (a dispatcher class or the lending service).
- **Reader notification preferences:** validation against the enabled set and per-reader storage; a disabled channel is rejected naming the channel.
- **Contact data access (`ReaderRepository`):** phone number from `ReaderDetails`, email from the `username` of `Reader`; replaced by a double in class tests.
- **`LendingServiceImpl` and REST workflow:** triggers notifications only through `NotificationService`; collaboration and system SUTs.

This refines the design in Extensibility.md and Configurability.md; it does not require testing the external providers themselves.

### Elements to analyse — Iteration 3

- **Policies (`StandardLendingPolicy`, `AdaptiveLendingPolicy`):** pure rule evaluation over a history snapshot and a parameter object; the main class and mutation SUTs.
- **Parameter objects:** constructor validation of the specification constraints (2.x, 3.5, 4.2); domain-like rules suitable for transparent-box tests.
- **Selector and selection rules:** ordered evaluation and Standard default; boundary cases of LP-SEL1.
- **History provider:** translation of stored lendings into the snapshot using the injected clock; needs a repository double or a test database.
- **`Lending` entity:** limit date, overdue days and fines currently computed with `LocalDate.now()`; affected domain class for transparent-box tests once the date is supplied.
- **`LendingServiceImpl` and REST workflow:** delegation, rejection reason and persistence; collaboration and system SUTs.

## Step 4 — Choose one or more design concepts that satisfy the inputs of the iteration

### Design alternatives — Iteration 1

Question: how can we control provider communication when testing each bibliographic adapter independently (QAS9)? Both alternatives retain the required class, collaboration, domain, mutation and system test categories.

| Alternative | Organization | Benefits | Costs / limitations |
|---|---|---|---|
| A — Inject the HTTP client's existing contract | Supply the selected HTTP client to the adapter and replace it in isolated tests. | Fewer application abstractions; uses an existing client API. | Tests may depend on client-specific call chains; changing clients can affect adapters and tests. Feasibility depends on the client chosen. |
| B — Inject a small application-owned transport contract | Give adapters a narrow request/response transport interface; a production implementation uses the HTTP client. Tests substitute that interface. | Simple controlled responses/errors; isolates adapter tests from HTTP-client details. | One extra interface and implementation; real transport integration still needs testing. |

### Selected alternative — B

B is the proposed design: a small shared transport contract makes provider responses and failures easy to control without exposing HTTP-client details in adapter class tests. The cost is an additional boundary to maintain. Keep it limited to the operations actually needed; its exact signature and HTTP client remain pending.

Test each adapter with a transport double and controlled mapping collaborators where applicable. Then test the real adapter, mapper and transport together against a local HTTP provider double. Test the running API separately. Source and merge dependencies of the coordinator are also injected, as already proposed in extensibility. Neither alternative removes required testing categories or replaces the real SUT with a mock.

The tactics below support B and the broader test-boundary design. They work together; they are not additional competing alternatives.

### Testability tactics — Supporting bibliographic testing

Source: faculty ADD material — Testability Tactics catalogue and Tactics (vi). Only the relevant tactics are selected below.

#### Selected catalogue tactics — Iteration 1

- **Abstract Data Sources:** use the source interface and proposed transport contract to supply controlled bibliographic data and errors instead of contacting live providers (QAS9).
- **Limit Structural Complexity:** separate coordination, provider mapping, merge and persistence so each responsibility has a clear test boundary (QAS9).
- **Limit Nondeterminism:** use fixed inputs/responses and an explicit source order; simulate failures instead of depending on network timing or changing provider data. Final merge rules still need agreement (QAS9).
- **Executable Assertions:** check expected fields, errors and relevant interactions with assertions that fail when the observed behaviour is wrong (QAS9).
- **Sandbox:** run broader tests with local provider doubles and separate test data/storage where needed. The real backend components inside the declared SUT still execute (QAS9).

#### Practical techniques — Faculty Tactics (vi)

- **Dependency Injection:** pass behavioural collaborators explicitly so tests can supply controlled replacements.
- **Isolation:** execute class tests without Spring, persistence or live provider access; use real collaboration in separately declared broader SUTs.
- **Mocking:** use mocks/stubs for collaborators while keeping the class under test real.

These techniques put the catalogue tactics into practice. Logging can help diagnose failures, but is optional supporting instrumentation and does not replace assertions. Fixed response fixtures are not claimed as a full Record/Playback mechanism.

### Supporting tactics and concepts

- **Separation of Concerns / Encapsulation:** keep provider communication, mapping and merge behind explicit boundaries.
- **Use of Interfaces:** reuse the source contract and add only the narrow transport boundary justified above; other interfaces need a concrete substitution need.
- **Controllability and observability:** tests supply known inputs and verify public outputs, errors or relevant side effects without exposing private state solely for tests.

### Design alternatives — Iteration 2

Question: how can notification delivery be observed in tests without sending real messages (QAS10)?

| Alternative | Organization | Benefits | Costs / limitations |
|---|---|---|---|
| A — Replace the channels with a recording double of `NotificationService` | Consumers depend only on `NotificationService`; tests inject a double that records reader, channel and content. | No new production element; follows EXT-NOT-03; simple assertions on who was notified; 0 real messages. | Does not execute the real channel implementations, which need their own tests. |
| B — Replace only the delivery client inside each channel | Each channel receives its mail/SMS/HTTP client by injection; tests replace that client. | Executes the real channel, including message building and contact lookup. | Needs one injectable delivery boundary per mechanism; consumer tests still depend on real channels. |
| C — Use real providers with a test account | Tests send real messages to a test mailbox, number or receiver. | Exercises real delivery. | Violates 0 real messages (QAS10); slow, non-repeatable, depends on external services. |

### Selected alternative — A

A is the proposed design: it is the approach QAS10 describes (channels replaced by doubles that record calls) and lets tests assert which channels were used, for which reader and with what content. The cost is that the channel implementations are not exercised by these tests and are verified separately. B is rejected because it ties every consumer test to real channel classes. C is rejected because it contradicts QAS10.

### Testability tactics — Notification testing

- **Dependency Injection:** `NotificationService`, `ReaderRepository` and the enabled-channels property are passed in, so tests supply controlled replacements.
- **Isolation:** class tests run without Spring, database or delivery providers.
- **Mocking (recording doubles):** replaced channels record reader, channel and content so tests can assert the calls made.
- **Limit Nondeterminism:** fixed readers, events and channel sets; no dependence on provider availability or timing.
- **Executable Assertions:** assert the exact channels, recipients and content, and that disabled or unchosen channels received 0 calls.
- **Sandbox:** collaboration and system tests use test properties, a test database with known readers and preferences, and recording doubles.

### Design alternatives — Iteration 3

Question: how can date-dependent lending rules be tested with a known "today" (QAS11)?

| Alternative | Organization | Benefits | Costs / limitations |
|---|---|---|---|
| A — Keep `LocalDate.now()` and mock it statically in tests | Static mocking of the time API in each test. | No production change. | Tests depend on a mocking-library feature; time stays hidden in many classes; easy to forget in broader tests; does not help system tests. |
| B — Inject a `java.time.Clock`; the history snapshot carries the reference date | The history provider, `Lending` creation and overdue checks obtain the date from one injected `Clock`; policies and rules read the date from the snapshot. | One explicit time source; policies stay pure and need no clock; collaboration and system tests use a fixed clock bean. | Production code changes once (constructors and the service); `Lending` methods that use `LocalDate.now()` need a date parameter or the clock. |
| C — Compute every date relative to the real date in assertions | Tests calculate expected values from the real current date. | No design change. | Boundary cases (overdue exactly today, window limits) cannot be fixed; results vary by run date; already the weakness of current tests. |

### Selected alternative — B

B is the proposed design: a single injected `Clock` makes "today" a controllable input, and the snapshot passes it to policies and rules so they remain free of time and framework dependencies (CON-LP-4). The cost is a one-time change to how `Lending` and the service obtain the date. A and C are rejected because they keep time implicit and cannot reproduce boundary dates.

#### Selected catalogue tactics — Iteration 3

- **Abstract Data Sources:** the history snapshot and parameter objects are the only data the rules read; tests build them directly instead of seeding a database (QAS11).
- **Limit Nondeterminism:** a fixed `Clock` and fixed parameter values make every date-dependent result reproducible (QAS11).
- **Limit Structural Complexity:** policy rules, selection, history collection and parameter validation are separate classes with separate test boundaries (QAS11).
- **Executable Assertions:** assert the decision (allowed/rejected), duration and reason, and the exact validation message, not only that a result exists.
- **Sandbox:** collaboration and system tests use a dedicated test database seeded with known histories, test properties and a fixed clock bean.

Dependency Injection, Isolation and Mocking (Tactics (vi)) put these into practice: parameters, history provider, selector and clock are injected; class tests run without Spring; the service is tested with a mocked selector and policy.

## Step 5 — Instantiate architectural elements, allocate responsibilities, and define interfaces

### Tactics and design decisions

| ID | Design decision | Rationale |
|---|---|---|
| TST-BIB-01 | Declare the SUT and real/replaced collaborators for every test group. | Prevent class tests from silently becoming real multi-class tests; follow the classroom convention. |
| TST-BIB-02 | Inject sources and merge; give provider adapters a narrow application-owned transport contract and controlled mapping collaborators where applicable. | Control responses/errors without coupling isolated adapter tests to HTTP-client internals (QAS9). Test the real transport separately and with adapters. |
| TST-BIB-03 | Use fixed provider-response fixtures and observable normalized results/outcomes. | Reproduce partial/invalid responses and verify mapping without relying on changing remote data. |
| TST-BIB-04 | Separate class, collaboration and system test groups; exercise the real production composition in broader tests. | Isolation alone cannot reveal wiring or contract incompatibilities (section 1.3.3.4). |
| TST-BIB-05 | Design representative cases with AAA and meaningful assertions, then analyse coverage and mutants. | Detect wrong behaviour rather than merely execute lines or count tests. |
| TST-NOT-01 | Consumers of notifications depend only on `NotificationService`; tests inject a recording double that stores reader, channel and content. | Observe who was notified without real delivery; follows EXT-NOT-03 (QAS10). |
| TST-NOT-02 | Give each channel implementation an injectable delivery boundary (mail, SMS or HTTP client) and its `ReaderRepository`; test each as a single-class SUT with those replaced. | Message building and contact lookup are verified without sending anything (QAS10, EXT-NOT-04). |
| TST-NOT-03 | Test channel composition with test properties: only listed identifiers are registered; empty or unknown identifiers behave as agreed. | Disabled channels send 0 notifications (QAS5, CFG-NOT-01). |
| TST-NOT-04 | Test preference validation and dispatch as separate class SUTs: a choice including a disabled channel is rejected naming it; dispatch uses the intersection of enabled and chosen channels. | One reader's choice does not affect another (QAS6, CFG-NOT-02, CFG-NOT-03). |
| TST-NOT-05 | Run collaboration and system tests with test properties, a test database with known readers and preferences, and recording or local delivery doubles. | Exercises real wiring and persistence while guaranteeing 0 real messages (QAS10). |
| TST-NOT-06 | Apply mutation testing to channel implementations, preference validation and dispatch; review surviving mutants on the intersection and on the enabled-set check. | A wrong set operation or a skipped check silently sends too many or too few messages. |
| TST-LP-01 | Inject one `java.time.Clock`; the history snapshot carries the reference date to policies and rules. | Reproducible boundary dates; policies stay free of time and Spring (QAS11, CON-LP-4). |
| TST-LP-02 | Test each policy, rule, the selector and each parameter object as a single-class SUT with fixed parameter objects and snapshot fixtures. | Every rule gets a passing and a failing case without Spring or database (QAS11). |
| TST-LP-03 | Test `LendingServiceImpl` with a mocked history provider, selector and policy; assert delegation, rejection reason and that nothing is saved when rejected. | Verifies the service does not contain policy rules (EXT-LP-05, QAS3). |
| TST-LP-04 | Test parameter validation at two levels: parameter-object constructors (class) and application startup with invalid test properties (collaboration). | Every constraint violation is detected and reported at startup (QAS7, LP-CFG2). |
| TST-LP-05 | Run collaboration and system tests with a dedicated test database seeded with known histories, test properties and a fixed clock bean. | Exercises real wiring, queries and binding without depending on the run date. |
| TST-LP-06 | Apply mutation testing to policies, rules, selector and parameter objects; review surviving mutants on boundary comparisons. | Off-by-one errors on thresholds and windows are the most likely defects (LP-A2, LP-SEL1). |

### Constraints

| ID | Constraint | Origin |
|---|---|---|
| CON-TST-1 | Represent all five requested testing categories, with transparent-box testing restricted to relevant domain classes. | Section 1.3.3.4 |
| CON-TST-2 | Bibliographic automated tests use controlled providers, not live Google/Open Library services. | QAS9 |
| CON-TST-3 | New author/genre records require explicit librarian action; incomplete books must not be saved. | US1 acceptance criteria / EXT-BIB-02 |
| CON-TST-4 | Explicitly distinguish a single-class SUT from real application-class collaboration. | Reported classroom guidance |
| CON-TST-5 | Preserve domain invariants while making dependencies explicit; no blanket mocking or weakening validation for coverage. | Proposed design safeguard |
| CON-TST-6 | Lending tests do not depend on the date they run; boundary dates are fixed through the injected clock. | QAS11 |
| CON-TST-7 | Tests affected by the open faculty questions (Adaptive count before/after, renewal duration, history window boundaries, overdue renewal under Adaptive) are written once the answers are known; no expected value assumes an answer. | ADD.md pending questions, CON-LP-3 |
| CON-TST-8 | Notification tests send 0 real messages; no real email, SMS or webhook provider is reachable from any automated test. | QAS10 |
| CON-TST-9 | Tests depending on undefined notification behaviour (channel failure, webhook target, behaviour with no enabled channel, trigger of due-date and overdue events) are written once those are decided; no expected value assumes an answer. | Extensibility.md and Configurability.md pending decisions |

### Responsibility assignment rationale

| Element | Responsibility / rationale | Properties | Related drivers |
|---|---|---|---|
| `BibliographicService` | Coordinate enabled sources and merge through replaceable contracts. | Single real coordinator in isolated tests | QAS9, CON-TST-4 |
| Provider adapter | Convert the lookup operation into a provider interaction and use controlled mapping/transport. | Adapter; exact collaborators to finalize | QAS9, CON-TST-2 |
| Provider mapper, if extracted | Translate provider fields to normalized partial metadata. | Test separately before adapter + mapper integration | QAS9, CON-TST-4 |
| `BibliographicMerge` | Apply agreed data precedence/combination without HTTP/persistence. | Deterministic class SUT | US3, QAS9 |
| Test fixtures and doubles | Supply explicit inputs and record relevant interactions. | Test-only code; no substitute for exercising the real SUT | CON-TST-2, CON-TST-4 |
| Registration domain classes | Enforce the rules assigned to each domain class. | Transparent-box candidates where actually affected | CON-TST-1, CON-TST-3 |

Names and proposed separations are not implemented contracts. Adapter/mapper/transport boundaries must be finalized before claiming isolated class coverage.

### Responsibility assignment rationale — Iteration 2

| Element | Responsibility / rationale | Properties | Related drivers |
|---|---|---|---|
| `NotificationService` | Common contract; the point where tests observe notifications. | Replaced by a recording double in consumer tests | QAS10, TST-NOT-01 |
| `SMSNotificationService`, `EmailNotificationService` | Build the message from the event and the reader's contact data (`ReaderDetails` phone, `Reader` username) and pass it to the delivery boundary. | Class SUT with repository and delivery doubles | QAS10, TST-NOT-02 |
| `WebhookNotificationService` | Build and send the webhook request. Target source pending. | Class SUT with delivery double; local receiver in collaboration | QAS10, TST-NOT-02 |
| Channel composition | Register only the enabled implementations. | Collaboration with test properties; class test of the selection logic if extracted | QAS5, TST-NOT-03 |
| Preference validation and storage | Reject disabled channels; store per reader. | Class SUT with repository double; collaboration with test database | QAS6, TST-NOT-04 |
| Dispatch | Compute the intersection of enabled and chosen channels and call each. | Class SUT with recording channels | QAS10, TST-NOT-04 |
| `LendingServiceImpl` | Trigger notifications through the contract only. | Class SUT with recording double | QAS2, TST-NOT-01 |
| Recording doubles and fixtures | Supply readers, channels and events; record calls. | Test-only; never replace the SUT | CON-TST-8 |

Names and separations are proposals; whether dispatch is a class of its own or part of the lending service is pending.

### SUT boundaries and test categories — Iteration 2

| Category | SUT / real production elements | Controlled outside the boundary | Verification |
|---|---|---|---|
| Functional opaque-box — classes | One channel, the dispatch, the preference validation or the lending service at a time | Recording `NotificationService`, delivery doubles, repository double, fixed enabled set | Channels called, recipient, content, rejection message |
| Functional transparent-box — domain classes | `Reader`/`ReaderDetails` contact fields used by the channels, if affected | Prepared readers | Branches on missing or invalid contact data; none if the iteration changes no domain rule |
| Mutation — classes | Channels, dispatch, preference validation | Same as class tests | Mutants on the intersection, enabled-set check and recipient choice are killed |
| Functional opaque-box — collaboration | Controller + real lending service, dispatch, channel composition, preference storage and repository | Test properties, test database with known readers, delivery doubles or local webhook receiver | Notifications recorded for a due or overdue lending, disabled channels called 0 times, invalid choice rejected |
| Functional opaque-box — system | Running backend through HTTP with security and persistence | Independent test data, test properties, delivery doubles | End-to-end preference change and its effect on later notifications |

Transparent-box testing is only used if a domain class is actually changed; do not invent domain logic for this category. Tests that run a real channel with a delivery double are class tests only if the repository is also replaced; otherwise they are collaboration tests.

### Planned cases — Arrange, Act, Assert — Iteration 2

| Case | SUT / level | Arrange → Act → Assert |
|---|---|---|
| Channel used | Dispatch / class | Recording channels for email, SMS, webhook; reader chose email and SMS, all enabled → notify → email and SMS recorded once with the reader and content, webhook 0 calls (US8, CFG-NOT-03). |
| Disabled channel | Dispatch / class | Reader chose SMS, only email enabled → notify → no channel called (empty intersection); behaviour beyond that waits for the failure decision (CFG-NOT-03). |
| Several channels | Dispatch / class | Reader chose all three, all enabled → notify → each called once (QAS10). |
| Preference rejected | Preference validation / class | Enabled set {email}; request {email, SMS} → validate → rejected with error naming SMS; stored preferences unchanged (CFG-NOT-02). |
| Preferences per reader | Preference storage / collaboration | Two readers; change one → read both → only the first changed (QAS6). |
| Contact data | `SMSNotificationService`, `EmailNotificationService` / class | Repository double returns a reader with a known phone and username → send → delivery double receives that phone or email and the content (EXT-NOT-04). Missing contact data waits for the failure decision. |
| Content | Each channel / class | Fixed due-date and overdue-with-fine events → send → message contains the lending identifier, the date and, for overdue, the fine (US6, US7). |
| Composition | Channel composition / collaboration | Property `email,sms` → start context → exactly those two implementations exist; webhook absent (CFG-NOT-01). Unknown identifier and empty value wait for the agreed behaviour. |
| Lending trigger | `LendingServiceImpl` / class | Recording `NotificationService`; lending due in a fixed number of days with the fixed clock → run the trigger → one notification for that reader; not-yet-due lending → none. Trigger mechanism pending. |
| Due and overdue end to end | Collaboration | Seeded readers, lendings and preferences, fixed clock, delivery doubles → run the trigger → recorded notifications match each reader's chosen and enabled channels; 0 real messages (QAS10). |
| Preference change | System | Change a reader's choice through the API → trigger a notification → only the new channels receive it, without restart (QAS6). |

### Technical Memo — Iteration 2

Testability — Notification methods (memo to be added in `Technical Memos/Testability/NotificationMethods.md`).

### Responsibility assignment rationale — Iteration 3

| Element | Responsibility / rationale | Properties | Related drivers |
|---|---|---|---|
| `StandardLendingPolicy`, `AdaptiveLendingPolicy` | Evaluate a request from a snapshot and parameters. | Pure class SUT; no clock, Spring or repository | QAS11, TST-LP-02 |
| Parameter objects | Validate their constraints on construction. | Transparent-box and mutation SUT | QAS7, TST-LP-04 |
| `LendingPolicySelector`, `AdaptiveEligibilityRule` | Choose the policy from the snapshot; Standard by default. | Class SUT with rule/policy doubles for the selector | QAS8, QAS11 |
| History provider | Build the snapshot from stored lendings and the clock. | Class SUT with repository double; collaboration with test database | QAS11, TST-LP-01 |
| `LendingServiceImpl` | Delegate and act on the decision. | Class SUT with mocked collaborators | QAS3, TST-LP-03 |
| Fixed clock and fixtures | Supply "today", histories and parameter values. | Test-only; never replaces the SUT | CON-TST-6 |

### SUT boundaries and test categories — Iteration 3

| Category | SUT / real production elements | Controlled outside the boundary | Verification |
|---|---|---|---|
| Functional opaque-box — classes | One policy, rule, selector, parameter object, history provider or the service at a time | Snapshot fixtures, fixed parameter objects, fixed clock, mocked collaborators | Allowed/rejected decision, duration, reason, selected policy, validation errors |
| Functional transparent-box — domain classes | Parameter objects and `Lending` (limit date, overdue days, fine) | Fixed dates; prepared collaborators | One case per branch and boundary of each constraint and date calculation |
| Mutation — classes | Policies, rules, selector, parameter objects | Same as class tests | Mutants on comparisons (`<`, `<=`), thresholds and default selection are killed |
| Functional opaque-box — collaboration | Controller + real service, provider, selector, policies, repository and parameter binding | Test database with seeded histories, test properties, fixed clock bean | HTTP result, persisted limit date, rejection reason, policy re-evaluated between two requests; invalid properties stop context startup |
| Functional opaque-box — system | Running backend through HTTP with security and persistence | Independent test data, test properties, fixed clock | End-to-end lending/rejection and the effect of a changed parameter after restart |

### Planned cases — Arrange, Act, Assert — Iteration 3

| Case | SUT / level | Arrange → Act → Assert |
|---|---|---|
| Standard active limit | `StandardLendingPolicy` / class | Snapshot with `MaxActiveLendings − 1` and then `MaxActiveLendings` active lendings → evaluate new lending → first allowed with `LendingPeriod`, second rejected with the limit reason (LP-S1, LP-S2). |
| Overdue blocks lending | Each policy / class | Snapshot with one overdue lending → evaluate new lending → rejected with the overdue reason (LP-S4, LP-A5). |
| Standard renewal | `StandardLendingPolicy` / class | Lending with `MaxRenewals` renewals, or overdue → evaluate renewal → rejected; below the limit and not overdue → allowed with `RenewalPeriod` (LP-S3, LP-S5). |
| Adaptive periods | `AdaptiveLendingPolicy` / class | Active counts just below and at `FirstThreshold` and `SecondThreshold` → evaluate → Long, Standard, Short periods (LP-A2). Which count is used depends on open question 1; expected values are set once answered. |
| Parameter constraints | Parameter objects / transparent-box | Values with `FirstThreshold >= SecondThreshold`, `SecondThreshold >= MaxActiveLendings`, `ShortPeriod >= StandardPeriod`, a zero period, `MinOnTimeRatio` outside 0–100 → construct → exception naming parameter and constraint; valid set → created (LP-A3, LP-CFG2). |
| Selection conditions | `AdaptiveEligibilityRule` / class | Snapshots with completed lendings `MinCompletedLendings − 1` and exactly the minimum, on-time ratio just below and exactly at `MinOnTimeRatio`, and a current overdue lending → evaluate → qualifies only when all hold (LP-SEL1). Window boundary dates wait for open question 4. |
| Default and order | `LendingPolicySelector` / class | Rule doubles: none matches; first matches; two match → select → Standard; first rule's policy; first in order. |
| Service delegation | `LendingServiceImpl` / class | Mocked selector returns a policy double that rejects → create → rejection carries the policy reason and nothing is saved; policy allows N days → saved lending with limit date = fixed today + N. |
| Re-evaluation per request | Collaboration | Seed a reader eligible for Adaptive; request a lending; add an overdue lending; request again → second request evaluated with Standard (LP-SEL2). |
| Invalid configuration | Collaboration (context startup) | Test properties with `FirstThreshold >= SecondThreshold` → start application context → startup fails with a message naming the parameter (QAS7). |
| Changed parameter | System | Start with `LendingPeriod = 21` in test properties → request a lending → limit date is fixed today + 21. |

### Technical Memo — Iteration 3

[Testability — Lending policies](Technical%20Memos/Testability/LendingPolicies.md)

### SUT boundaries and test categories

| Category | SUT / real production elements | Controlled outside the boundary | Verification |
|---|---|---|---|
| Functional opaque-box — classes | One coordinator, merge, mapper or adapter class at a time | Behavioural collaborators replaced; explicit data fixtures | Public contract, accepted/rejected cases, missing fields and outcome handling |
| Functional transparent-box — domain classes | One relevant affected domain class at a time | Collaborators prepared/controlled according to the classroom boundary | Cases derived from branches and boundaries of actual domain rules; outputs/exceptions remain the oracle |
| Mutation — classes | Selected new/changed class behaviour exercised by functional tests | Same controlled dependencies | Whether assertions detect changed mapping, predicates or merge choices |
| Functional opaque-box — collaboration | Controller + real service + selected real domain/repository/gateway components; explicitly list included components for each suite | Remote provider replaced by local HTTP double; dedicated test database when real repository included | Real calls, wiring, mapping and persistence effects; test the included classes separately first |
| Functional opaque-box — system | Running backend through HTTP, including security, configuration and persistence | Remote providers controlled; independent test data/storage | End-to-end retrieval/registration results and relevant error responses |

Class isolation does not mean every DTO must become an interface. If its constructor performs application validation/mapping relevant to the test, document and separate that behaviour or classify the wider SUT honestly. HTTP-double tests involving a real adapter, mapper and client are collaboration tests, not a single-class adapter test.

Bibliographic infrastructure is not domain merely because it contains branches. Do not invent a domain entity just to fill the transparent-box category. Identify affected domain rules during registration integration; if this iteration changes none, record that fact and cover new policy/domain rules in the later iteration.

### Planned cases — Arrange, Act, Assert

| Case | SUT / level | Arrange → Act → Assert |
|---|---|---|
| Enabled sources only | Coordinator / class | Prepare source doubles and merge double → retrieve ISBN → assert the specified result, calls to enabled sources and no calls to disabled sources. |
| Partial response | Provider mapper / class | Supply a fixed response missing optional fields → map → assert exact available/missing values and provenance. |
| Data combination | Merge / class | Prepare partial normalized inputs → combine → assert field selection after precedence/list rules are agreed. |
| No match versus failure | Adapter/coordinator / separate class tests | Supply controlled no-match/error outcomes → execute → assert their distinct agreed contracts. |
| Missing catalogue association | Registration collaboration | Provide metadata without a required association → submit → assert completion/error response and no book/author/genre created implicitly. |
| Complete registration | Registration collaboration/system | Prepare valid data, existing associations and controlled provider responses → submit → assert persisted book and API result; no extra completion when unambiguous. |

Do not assert only `notNull`, hide the sole assertion inside an optional branch, or assert a value computed by the same production logic. For expected errors, the Act may be inside `assertThrows`; AAA need not be three literal code blocks. Opaque versus transparent describes how cases are derived, not whether the SUT runs real code.

### Tools and acceptance evidence

- Existing JUnit/Mockito facilities are candidates for class tests; Spring test support for real collaboration. Select a local HTTP double when provider-client boundaries are finalized.
- Surefire executes the configured test selection; Failsafe can separate integration execution if explicitly configured. A test filename alone is not SUT evidence.
- JaCoCo measures exercised code. The reported final target is at least 85%; line coverage is the current assumption and measurement scope still needs confirmation. QAS9's 80% branch target is a separate group proposal.
- PIT mutation testing requires later configuration. Surviving mutants are not failing functional tests; mutation score and any exclusions require explicit justified criteria.
- Tests of error paths must pass when expected rejection occurs. No rule accepting integration failures is set here: any faculty-required tolerance needs a separately agreed criterion.

### Technical Memo

[Testability — Bibliographic sources](Technical%20Memos/Testability/BibliographicSources.md)

## Step 6 — Sketch views and record decisions

### Class isolation boundary

![Bibliographic class isolation boundary](Images/BibliographicClassIsolation.png)

### Collaboration boundary

![Bibliographic collaboration boundary](Images/BibliographicCollaborationBoundary.png)

The second sketch concerns retrieval; registration suites extend the declared SUT with the relevant domain and repository components. System tests invoke a running backend via HTTP instead of invoking the controller directly. These are test-boundary sketches, not replacements for full implementation views.

### Lending policy class isolation boundary

![Lending policy class isolation boundary](Images/LendingPolicyClassIsolation.png)

### Lending policy collaboration boundary

![Lending policy collaboration boundary](Images/LendingPolicyCollaborationBoundary.png)

Class tests build the snapshot and parameter objects directly; only the selector uses rule and policy doubles. The collaboration boundary runs the real service, provider, selector, policies, repository and parameter binding against a seeded test database, with the clock and properties controlled from outside.

### Notification boundary

No sketch has been drawn yet. Class tests place one channel, the dispatch or the lending service inside the boundary, with the recording `NotificationService`, delivery boundary and repository outside it. The collaboration boundary adds the real lending service, dispatch, composition, preference storage and repository, with test properties and a test database; delivery stays replaced. System tests invoke the running backend over HTTP. Diagrams for these two boundaries follow the style of the bibliographic and lending sketches.

**Pending decisions (notifications):** form of the delivery boundary per channel; where dispatch lives; how due-date and overdue events are triggered; webhook target; channel failure behaviour and behaviour with no enabled channel; fixture format for readers and preferences; whether context tests run under Surefire or Failsafe.

**Pending decisions:** expected values affected by the open faculty questions; fixture format for seeded histories; whether context-startup tests run under Surefire or Failsafe; mutation-score threshold and exclusions.

## Step 7 — Perform analysis of current design and review iteration goal and design objectives

### Design Kanban

The columns are independent status lists: cells on the same row are not related. This board tracks testability design in this document, not implemented tests or design work in other documents.

| Not addressed | Partially addressed | Addressed in the design |
|---|---|---|
| | Bibliographic isolation — QAS9: SUT boundaries, tactics and test categories proposed; concrete contracts, fixture examples and expected results still need refinement |  |
| | Notification testability — QAS10: recording doubles, delivery boundary, SUT boundaries, test categories and AAA cases proposed; dispatch location, event trigger, failure behaviour and boundary sketches still pending | |
| | Lending policy testability — QAS11: clock injection, SUT boundaries, test categories and AAA cases proposed; cases affected by open faculty questions and fixture formats still pending | |

## Step 8 — Refine as necessary

Resolve the pending contracts with concrete examples; update the memo and boundary sketches; implement a small class-level case and then its collaboration test. Refine existing tests only where relevant, without mass-refactoring every module. Continue with the notification and lending iterations after reviewing their drivers.

For notifications: decide the delivery boundary, the dispatch location and the failure behaviour first; then write the recording-double class tests for dispatch and preference validation, followed by one channel at a time, and only then the composition and lending-trigger collaboration tests. Draw the notification boundary sketches and add the testability memo.

For lending: introduce the clock first, then add class tests for parameter objects and policies before refactoring `LendingServiceImpl`; keep the existing lending tests passing as a regression check, and update the expected values once the faculty answers the open questions.
