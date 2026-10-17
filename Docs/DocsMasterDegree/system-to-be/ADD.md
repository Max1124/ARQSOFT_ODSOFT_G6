# ADD

This file details the ADD process used to develop new functionalities.

## Design iterations

- [Extensibility — roadmap and iteration 1: bibliographic sources](Extensibility.md): three planned iterations, for bibliographic information, notifications and lending policies. Iteration 1 groups source extension, composition, configuration and test isolation; only its proposed design is detailed so far. Not yet implemented.

- [Testability — roadmap and iteration 1: bibliographic sources](Testability.md): proposed SUT boundaries, isolation tactics, test categories, AAA scenarios and evaluation. Notifications and lending policies remain planned; no test implementation is claimed.

## Requirements
Below are listed the non-functional and functional requirements

### Non-functional requirements
#### Bibliographic information
- The system must gather book data from external systems, such as Google Books and Open Library.
- The system must account for different external systems may provide different data models and
  provide different subsets of bibliographic information.
- The systems must support what external book information systems are consulted at setup.
- The system must allow consultation of more than one external book information system at a time.
- The system must support new bibliographic information sources with limited impact on existing
  functionality.

#### Reader notifications
- Reader notifications must be done via email, SMS and webhook.
- Reader notifications may use more than one method at a time. 
- At setup time, the system must allow what notification systems are to be used.
- New notification systems should have the possibility to be added without impacting existing functionality.

#### Lending policies
- The system must support different lending policies according to the complementary lending policy
  specification.
- The applicable lending policy must be selected automatically for each reader on every new lending or renewal request, according to the faculty selection rules.
- The system must support addition of new lending policies with limited impact on existing functionality.

### Functional requirements
#### Bibliographic information
- US1: As a Librarian, I want to create a book using its ISBN, so that the system retrieves and fills in the available bibliographic information from the configured external sources, reducing manual entry.
- US2: As a Librarian, I want to search for a book's bibliographic information by ISBN or title in the external sources, before registering it in the library catalogue.
- US3: As a Librarian, I want the system to combine the data returned by several external sources into a single book record when more than one source is configured, filling missing fields from whichever source provides them.
- US4: As a Librarian, I want to be able to add more new bibliographic information sources.
- US5: As an Administrator, I want to define at setup time which bibliographic information sources are enabled, without changing the source code.

#### US1 acceptance criteria
- AC1: The system fills in available bibliographic fields from the configured sources.
- AC2: Existing catalogue authors and genre may be associated automatically only when the correspondence is unambiguous. Matching rules must be defined and tested; a name match alone is not assumed sufficient.
- AC3: Missing required data or ambiguous associations are reported to the librarian for completion or confirmation. No incomplete book is saved.
- AC4: Authors and genres absent from the catalogue are created only through an explicit librarian decision, not automatically during retrieval.
- AC5: When metadata and catalogue associations are sufficient and unambiguous, registration requires no additional manual completion.

#### Reader notifications
- US6: As a Librarian, I want the Reader to be notified when a lending is about to reach its due date, through the notification channels enabled in the system.
- US7: As a Librarian, I want the Reader to be notified when a lending is overdue and a fine is being applied.
- US8: As a Reader, I want to choose through which of the enabled channels (email, SMS, webhook) I receive notifications, being able to select more than one at a time.
- US9: As an Administrator, I want to define at setup time which notification systems are enabled, without changing the source code.

#### Lending policies
- US11: As a Reader, I want my applicable lending policy to be selected automatically from my history and current lending status on every new lending or renewal request.
- US12 (group proposal): As a Librarian, I want to consult the policy applicable to a reader and its parameters. A consultation endpoint is additional scope, not a requirement for a global active policy.
- US13: As a Reader, I want new lendings to follow the applicable Standard or Adaptive policy and receive a reason when rejected.
- US14: As a Reader, I want to renew a lending only when the re-evaluated policy permits it.

### Lending rules from the faculty specification
Scope for this two-person group: section 5 (external setup-time parameter configuration) is excluded following the reported faculty instruction. Section 4 (automatic policy selection) remains included pending faculty confirmation. US10 and QAS7 are retired; other identifiers are retained to preserve references. Numeric conditions in sections 2-4 still apply, without requiring external configuration or startup validation.
- LP-S1 (2.1): Standard permits at most `MaxActiveLendings > 0` active lendings.
- LP-S2 (2.2): Standard lending duration is `LendingPeriod > 0` days.
- LP-S3 (2.3): Standard permits at most `MaxRenewals >= 0` renewals, each for `RenewalPeriod > 0` days.
- LP-S4 (2.4): A reader with any overdue lending cannot start a new Standard lending.
- LP-S5 (2.5): An overdue lending cannot be renewed under Standard.
- LP-A1 (3.1): Adaptive permits at most `MaxActiveLendings > 0` active lendings.
- LP-A2 (3.2-3.4): Below `FirstThreshold`, use `LongPeriod`; from `FirstThreshold` inclusive to `SecondThreshold` exclusive, use `StandardPeriod`; at or above `SecondThreshold`, use `ShortPeriod`.
- LP-A3 (3.5): Validate `0 < FirstThreshold < SecondThreshold < MaxActiveLendings` and `LongPeriod > StandardPeriod > ShortPeriod > 0`.
- LP-A4 (3.6): Adaptive renewal requires the resulting lending to continue satisfying the applicable policy conditions; the exact calculation needs clarification.
- LP-A5 (3.7): A reader with any overdue lending cannot start a new Adaptive lending.
- LP-SEL1 (4.1-4.2): Standard is the default. Adaptive applies only when, in the previous `HistoryPeriod > 0` months, the reader completed at least `MinCompletedLendings > 0`, returned at least `MinOnTimeRatio` (0-100%) of those lendings on time, and has no currently overdue lending. All conditions must hold.
- LP-SEL2 (4.3): Re-evaluate selection for each reader on every new lending and renewal request.

### Scope and classification notes
The existing non-functional section mixes required functional capabilities with quality requirements. Retrieving external data and sending notifications are functional; limited impact of additions is extensibility; setup-time selection is configurability. User stories refine these capabilities.

Search by title (US2), merge behaviour (US3), specific notification events (US6/US7), reader preferences (US8), and policy consultation (US12) are group proposals requiring scope confirmation. Coverage percentages, effort estimates and timings in the QAS are group targets, not faculty requirements.

### Pending questions for the faculty
1. Does Adaptive duration use the active-lending count before or after adding the requested lending?
2. What duration does an Adaptive renewal add, from which date, and is there a renewal-count limit?
3. If the selected policy changes on renewal, how do its rules interact with original terms and previous renewals?
4. Which date determines inclusion in the completed-lending history window, and are boundary dates inclusive?
5. Can an overdue lending be renewed under Adaptive? Standard forbids it explicitly (specification 2.5); Adaptive only states that the resulting lending must continue to satisfy the policy conditions (3.6).

These questions remain open; affected calculations and acceptance tests must not assume answers.


## Quality Attribute Scenarios
The Library Management service does not adequately support **Extensibility**, **Configurability** and **Testability**. The following scenarios make these qualities concrete and measurable.

### Extensibility

#### QAS1 – Adding a new bibliographic information source
**Related requirements:** US4, NFR "support new bibliographic information sources with limited impact"

| Element | Statement |
|---|---|
| Stimulus | A new external bibliographic information source (e.g., ISBNdb) must be supported, with its own data model and its own subset of bibliographic fields. |
| Stimulus source | Developer, following a request from the Librarian / library management. |
| Environment | Design and development time, with the system already integrating Google Books and Open Library. |
| Artifact | Book management module: bibliographic source abstraction, source adapters and the data mapping to the internal `Book` model. |
| Response | The developer adds a new adapter that implements the existing source interface and maps the external data model to the internal one. Existing sources, `BookService` and `BookController` remain unchanged. |
| Response measure | Only new classes are created (one adapter and one mapper/DTO, at most). No existing class is modified. 100% of the existing tests still pass. Effort is at most 1 person-day. |

#### QAS2 – Adding a new notification channel
**Related requirements:** US6, US7, US8, NFR "new notification systems added without impacting existing functionality"

| Element | Statement |
|---|---|
| Stimulus | A new notification channel (e.g., push notifications) must be added alongside email, SMS and webhook. |
| Stimulus source | Developer, following a request from library management. |
| Environment | Design and development time, with email, SMS and webhook channels already in production. |
| Artifact | Reader notification module: notification channel interface and channel implementations. |
| Response | The developer implements the channel interface in a new class. The notification dispatcher discovers it and sends through it together with the other enabled channels. Readers can then select it as one of their channels. |
| Response measure | One new interface plus specific implementations for at least SMS and e-mail. No changes to existing channels, the dispatcher, or the lending module. 100% of the existing tests still pass. Effort is at most 2 person-day. |

#### QAS3 – Adding a new lending policy
**Related requirements:** US13, NFR "addition of new lending policies with limited impact"

| Element | Statement |
|---|---|
| Stimulus | A new lending policy defined in the complementary lending policy specification must be supported. |
| Stimulus source | Developer, following a decision from library management. |
| Environment | Design and development time, with Standard and Adaptive already implemented and selected separately for each reader. |
| Artifact | Lending management module: lending policy interface, policy implementations, `LendingServiceImpl`. |
| Response | The developer creates a new policy implementation. `LendingServiceImpl` validates new lendings through the policy interface, so it does not need to change. |
| Response measure | Add the new policy, parameter definitions and tests; extend selection registration/rules where needed. Existing policies, `LendingServiceImpl` and `LendingController` remain unchanged. 100% of existing tests still pass. The 1 person-day target is a group estimate. |

### Configurability

#### QAS4 – Choosing bibliographic sources at setup
**Related requirements:** US5, NFR "support what external book information systems are consulted at setup", "consultation of more than one system at a time"

| Element | Statement |
|---|---|
| Stimulus | The Administrator wants to enable Open Library and disable Google Books, or enable both at the same time. |
| Stimulus source | Administrator. |
| Environment | Setup / deployment time. |
| Artifact | External configuration (`application.properties`, environment variables or profile) and the source selection mechanism. |
| Response | At startup, the system reads the configuration and only queries the enabled sources. When more than one is enabled, it queries all of them and merges the results (US3). |
| Response measure | 0 lines of source code changed and no recompilation. Takes effect after one application restart. The change takes at most 5 minutes. |

#### QAS5 – Choosing notification channels at setup
**Related requirements:** US9, NFR "at setup time, the system must allow what notification systems are to be used"

| Element | Statement |
|---|---|
| Stimulus | The Administrator wants to enable only email and webhook notifications. |
| Stimulus source | Administrator. |
| Environment | Setup / deployment time. |
| Artifact | External configuration and the notification channel registry. |
| Response | Only the enabled channels are loaded. Readers can only select among enabled channels, and notifications go out only through them. |
| Response measure | 0 lines of source code changed and no recompilation. Takes effect after one application restart. Disabled channels send 0 notifications. |


#### QAS6 – Reader selecting notification channels at runtime
**Related requirements:** US8, NFR "reader notifications may use more than one method at a time"

| Element | Statement |
|---|---|
| Stimulus | A Reader changes their notification preferences, e.g., from email only to SMS and webhook. |
| Stimulus source | Reader, through the REST API. |
| Environment | Runtime, under normal operation, with email, SMS and webhook enabled at setup. |
| Artifact | Reader management module (reader notification preferences) and the notification dispatcher. |
| Response | The system validates that every chosen channel is enabled and stores the preferences. Later notifications for that Reader go only through the chosen channels. A choice that includes a disabled channel is rejected with an error message naming that channel. |
| Response measure | 0 code changes and 0 restarts. The new preferences apply to the next notification sent to that Reader. 100% of choices that include a disabled channel are rejected. Other Readers' preferences are unaffected. |

#### QAS8 – Automatic per-reader policy selection at runtime
**Related requirements:** US11, US14, LP-SEL1, LP-SEL2

This scenario specifies runtime selection behaviour, rather than setup-time reconfiguration.

| Element | Statement |
|---|---|
| Stimulus | A reader requests a new lending or renewal. |
| Stimulus source | Reader, through the lending REST API. |
| Environment | Runtime, under normal operation, with active lendings and concurrent requests. |
| Artifact | Lending service, policy selector and reader-history access. |
| Response | Evaluate all Adaptive eligibility conditions for this reader. Select Adaptive only when all hold; otherwise select Standard. Validate the requested operation with that policy. |
| Response measure | Every new lending and renewal re-evaluates selection, without restart or manual switching. Tests exercise each condition and readers with different eligibility. Selection alone does not modify existing lendings; renewal effects require clarification. |

### Testability

#### QAS9 – Testing bibliographic sources in isolation
**Related requirements:** US1, US2, US3

| Element | Statement |
|---|---|
| Stimulus | A developer needs to test book creation by ISBN and the merging of data from several sources. |
| Stimulus source | Developer / CI pipeline. |
| Environment | Development and continuous integration, without network access to Google Books or Open Library. |
| Artifact | Book management module: source adapters, merge logic, `BookService`. |
| Response | Sources are injected through their interface, so they can be replaced by mocks or stubs. Adapters are tested against recorded responses, and the merge logic is tested with controlled data, including partial and missing fields. |
| Response measure | 100% of tests run without real external calls. At least 80% branch coverage on the adapters and merge logic. Each unit test runs in under 1 second. |

#### QAS10 – Testing notification channels without sending real messages
**Related requirements:** US6, US7, US8

| Element | Statement |
|---|---|
| Stimulus | A developer needs to verify that a Reader with several selected channels is notified through all of them when a lending is due or overdue. |
| Stimulus source | Developer / CI pipeline. |
| Environment | Development and continuous integration, without email/SMS providers or webhook receivers. |
| Artifact | Reader notification module: dispatcher and channel implementations. |
| Response | Channels are replaced by test doubles that record the calls. Tests assert which channels were used, for which reader, and with what content. |
| Response measure | 0 real messages sent during tests. At least 80% branch coverage on the dispatcher and channel selection logic. Each scenario is checked by at least one automated test. |

#### QAS11 – Testing lending policies independently
**Related requirements:** US11, US13, US14

| Element | Statement |
|---|---|
| Stimulus | A developer needs to verify both policies, parameter constraints, renewals and automatic per-reader selection. |
| Stimulus source | Developer / CI pipeline. |
| Environment | Development and continuous integration. |
| Artifact | Lending policy implementations, policy selector and `LendingServiceImpl`. |
| Response | Each policy is tested as a standalone unit with injected parameters and a controllable clock, without starting Spring or the database. `LendingServiceImpl` is tested with a mocked policy. |
| Response measure | Every policy rule has at least one passing and one failing test case. At least 80% branch coverage on policy classes. Policy unit tests run in under 5 seconds in total. |


## Prioritization

This ranking is provisional. US14 and lending QAS need inclusion in the next prioritization review. Performance and Releasability require explicit scenarios before being used as architectural drivers. The scoring formula also remains to be defined.

The table below shows what we determined to be the priority of features to implement according to the ADD process:

|Requirements|Risk/Difficulty|Importance/Probability|Priority|
|-|-|-|-|
|Performance|<span style="color:red">High</span>|<span style="color:green">Low</span>|<span style="color:orange">3</span>|
|Releasability|<span style="color:red">High</span>|<span style="color:red">High</span>|<span style="color:red">4</span>|
|US1|<span style="color:red">High</span>|<span style="color:red">High</span>|<span style="color:red">4</span>|
|US2|<span style="color:green">Easy</span>|<span style="color:red">High</span>|<span style="color:yellow">2</span>|
|US3|<span style="color:red">High</span>|<span style="color:red">High</span>|<span style="color:red">4</span>|
|US4|<span style="color:yellow">Medium</span>|<span style="color:red">High</span>|<span style="color:orange">3</span>|
|US5|<span style="color:green">Easy</span>|<span style="color:red">High</span>|<span style="color:yellow">2</span>|
|US6|<span style="color:green">Easy</span>|<span style="color:red">High</span>|<span style="color:yellow">2</span>|
|US7|<span style="color:green">Easy</span>|<span style="color:red">High</span>|<span style="color:yellow">2</span>|
|US8|<span style="color:green">Easy</span>|<span style="color:red">High</span>|<span style="color:yellow">2</span>|
|US9|<span style="color:yellow">Medium</span>|<span style="color:red">High</span>|<span style="color:orange">3</span>|
|US11|<span style="color:red">High</span>|<span style="color:red">High</span>|<span style="color:red">4</span>|
|US12|<span style="color:green">Easy</span>|<span style="color:red">High</span>|<span style="color:yellow">2</span>|
|US13|<span style="color:green">Easy</span>|<span style="color:red">High</span>|<span style="color:yellow">2</span>|
