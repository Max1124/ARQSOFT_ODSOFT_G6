# ADD

This file details the ADD process used to develop new functionalities.

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
- The system must support externally configurable lending policy parameters at setup time, without requiring
  changes to the source code.
- The current lending policy must be changeable at runtime.
- The system must support addition of new lending policies with limited impact on existing functionality.

### Functional requirements
#### Bibliographic information
- US1: As a Librarian, I want to create a book by providing only its ISBN, so that the system fills in the remaining bibliographic data retrieved from the configured external sources.
- US2: As a Librarian, I want to search for a book's bibliographic information by ISBN or title in the external sources, before registering it in the library catalogue.
- US3: As a Librarian, I want the system to combine the data returned by several external sources into a single book record when more than one source is configured, filling missing fields from whichever source provides them.
- US4: As a Librarian, I want to be able to add more new bibliographic information sources.
- US5: As an Administrator, I want to define at setup time which bibliographic information sources are enabled, without changing the source code.

#### Reader notifications
- US6: As a Librarian, I want the Reader to be notified when a lending is about to reach its due date, through the notification channels enabled in the system.
- US7: As a Librarian, I want the Reader to be notified when a lending is overdue and a fine is being applied.
- US8: As a Reader, I want to choose through which of the enabled channels (email, SMS, webhook) I receive notifications, being able to select more than one at a time.
- US9: As an Administrator, I want to define at setup time which notification systems are enabled, without changing the source code.

#### Lending policies
- US10: As an Administrator, I want to configure the lending policy parameters (e.g., maximum lending duration, maximum simultaneous lendings, fine per day of delay) at setup time through external configuration, without changing the source code.
- US11: As a Librarian, I want to change the active lending policy while the system is running, so that new lendings follow the new policy immediately.
- US12: As a Librarian, I want to consult which lending policy is currently active and its parameters.
- US13: As a Reader, I want to be able to lend a book only if I comply with the rules of the currently active lending policy, and be told which rule I violate otherwise.


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
| Response measure | One new class plus its configuration entry. No changes to existing channels, the dispatcher, or the lending module. 100% of the existing tests still pass. Effort is at most 1 person-day. |

#### QAS3 – Adding a new lending policy
**Related requirements:** US13, NFR "addition of new lending policies with limited impact"

| Element | Statement |
|---|---|
| Stimulus | A new lending policy defined in the complementary lending policy specification must be supported. |
| Stimulus source | Developer, following a decision from library management. |
| Environment | Design and development time, with other lending policies already implemented and one of them active. |
| Artifact | Lending management module: lending policy interface, policy implementations, `LendingServiceImpl`. |
| Response | The developer creates a new policy implementation. `LendingServiceImpl` validates new lendings through the policy interface, so it does not need to change. |
| Response measure | One new policy class plus its parameter definitions. No changes to `LendingServiceImpl`, `Lending`, `LendingController` or the other policies. 100% of the existing tests still pass. Effort is at most 1 person-day. |

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

#### QAS7 – Configuring lending policy parameters at setup
**Related requirements:** US10, NFR "externally configurable lending policy parameters at setup time"

| Element | Statement |
|---|---|
| Stimulus | The Administrator wants to change the maximum lending duration from 15 to 21 days and the fine per day of delay. |
| Stimulus source | Administrator. |
| Environment | Setup / deployment time. |
| Artifact | External configuration and the lending policy parameter binding. |
| Response | At startup, the system loads the new values. New lendings use the new duration, and fines are calculated with the new value. Invalid values are rejected at startup with a clear error message. |
| Response measure | 0 lines of source code changed and no recompilation. Takes effect after one application restart. 100% of invalid parameter values are detected at startup. |

#### QAS8 – Changing the active lending policy at runtime
**Related requirements:** US11, US12, NFR "the current lending policy must be changeable at runtime"

| Element | Statement |
|---|---|
| Stimulus | The Librarian switches the active lending policy to a different one. |
| Stimulus source | Librarian, through the REST API. |
| Environment | Runtime, under normal operation, with active lendings and concurrent requests. |
| Artifact | Lending management module: active policy holder and the policy management endpoint. |
| Response | The system switches the active policy without a restart. Lending requests created after the switch are validated against the new policy. Existing lendings keep the terms they were created with. The currently active policy can be consulted (US12). |
| Response measure | 0 restarts and 0 downtime. The new policy applies to the next lending request, within 1 second. 0 existing lendings are changed. |

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
**Related requirements:** US10, US11, US13

| Element | Statement |
|---|---|
| Stimulus | A developer needs to verify each lending policy's rules, its parameters and runtime policy switching. |
| Stimulus source | Developer / CI pipeline. |
| Environment | Development and continuous integration. |
| Artifact | Lending policy implementations, parameter configuration and `LendingServiceImpl`. |
| Response | Each policy is tested as a standalone unit with injected parameters and a controllable clock, without starting Spring or the database. `LendingServiceImpl` is tested with a mocked policy. |
| Response measure | Every policy rule has at least one passing and one failing test case. At least 80% branch coverage on policy classes. Policy unit tests run in under 5 seconds in total. |


## Prioritization

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
|US10|<span style="color:yellow">Medium</span>|<span style="color:red">High</span>|<span style="color:orange">3</span>|
|US11|<span style="color:red">High</span>|<span style="color:red">High</span>|<span style="color:red">4</span>|
|US12|<span style="color:green">Easy</span>|<span style="color:red">High</span>|<span style="color:yellow">2</span>|
|US13|<span style="color:green">Easy</span>|<span style="color:red">High</span>|<span style="color:yellow">2</span>|