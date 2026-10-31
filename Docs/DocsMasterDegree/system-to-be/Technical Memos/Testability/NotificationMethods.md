# Technical Memo — TST-NOT: Notification testability

To see the implementation: [implemented contracts and verification](../../NotificationImplementation.md). The decisions below describe the implementation; the report records contracts and evidence.

Related iteration: [Testability — Notification methods](../../Testability.md) (iteration 2). Primary driver: QAS10. Related: QAS2, QAS5, QAS6; US6, US7, US8, US9. Design under test: [Extensibility — Notification Methods](../Extensibility/NotificationMethods.md) (EXT-NOT-01 to EXT-NOT-07) and [Configurability — Notification Methods](../Configurability/NotificationMethods.md) (CFG-NOT-01 to CFG-NOT-04).

## Issue

The notification module does not exist yet. The extensibility design introduces a common `NotificationService` contract with SMS, email and webhook implementations that perform their own delivery and read contact data through `ReaderRepository`. The configurability design adds a setup property listing the enabled channels, reader preferences validated against that set, and dispatch through the intersection of enabled and chosen channels. Tests of this behaviour must not send real messages, and the class, domain, mutation, collaboration and system scopes of section 1.3.3.4 must be represented.

## Problem

How can each notification class be tested on its own, and then their real collaboration and the running system, so that tests show which channels were used, for which reader and with what content, without sending any real message?

## Summary of solution

Consumers of notifications are tested with recording doubles of `NotificationService`. Each channel implementation is tested as a single-class SUT with `ReaderRepository` replaced and its internal delivery controlled. Channel composition, preference validation and dispatch are tested against the agreed configurability rules. Collaboration and system tests run with test properties, a test database and recording doubles or local receivers.

## Adopt the following tactics

Selected catalogue tactics: Abstract Data Sources, Limit Nondeterminism, Executable Assertions and Sandbox, put into practice with Dependency Injection, Isolation and Mocking:

- Dependency Injection: `NotificationService`, `ReaderRepository` and the enabled-channels property are passed in, and the composition injects the enabled channel instances into `LendingServiceImpl` (CFG-NOT-01), so tests supply controlled replacements.
- Isolation: execute class tests without Spring, persistence or live provider access; use real collaboration in separately declared broader SUTs.
- Mocking (recording doubles): replaced channels record reader, channel and content so tests can assert the calls made.
- Limit Nondeterminism: fixed readers, events, channel sets and a fixed clock; no dependence on provider availability or timing.
- Executable Assertions: assert the exact channels, recipients and content, and that disabled or unchosen channels received 0 calls.
- Sandbox: collaboration and system tests use test properties, a test database with known readers and preferences, and recording doubles or local receivers.

These are the tactics selected in Step 4 of [Testability.md](../../Testability.md), iteration 2. They preserve the contract and implementations agreed in extensibility and the configuration rules agreed in configurability; no production element is added only for testing.

## Factors

- QAS10 requires 0 real messages and at least 80% branch coverage on the dispatcher and channel selection logic.
- Each implementation keeps its protocol and delivery inside its own class (EXT-NOT-02, CON-NOT-5), so channel tests cannot rely on an application-owned delivery interface.
- `LendingServiceImpl` depends only on `NotificationService` (EXT-NOT-03); it receives the enabled channels from the composition, notifies through those the reader has chosen and runs the periodic due-date and overdue check (EXT-NOT-05, CFG-NOT-03).
- Contact data comes from `ReaderRepository` (EXT-NOT-04), which can be replaced by a double.
- Enabled channels are set at setup and an unknown identifier stops startup; reader preferences are runtime data stored per reader in `ReaderDetails` (CFG-NOT-01, CFG-NOT-02).
- Faculty section 1.3.3.4 distinguishes class, domain, mutation, collaboration and system scopes. Single-class isolation is the classroom criterion; test names, Spring annotations and typed variables alone do not establish the SUT.
- Channel failure behaviour, the behaviour with no enabled channel, the webhook target and the schedule of the due-date and overdue check were not yet decided when this design was made; they are resolved below.

## Solution

TST-NOT-01 (implemented): Consumers of notifications depend only on `NotificationService`; tests inject a recording double that stores reader, channel and content.

TST-NOT-02 (implemented): Each channel implementation is tested as a single-class SUT with its `ReaderRepository` replaced and its delivery, which stays inside the class, controlled as decided in Step 4 of [Testability.md](../../Testability.md).

TST-NOT-03 (implemented): Channel composition is tested with test properties: only listed identifiers are registered; an unknown identifier fails startup; the empty value stops startup.

TST-NOT-04 (implemented): Preference validation and dispatch are tested as separate class SUTs: an empty choice is rejected and a choice including a disabled channel is rejected naming it; dispatch, in `LendingServiceImpl`, uses the intersection of enabled and chosen channels and is tested with one recording `NotificationService` double per channel and a `ReaderRepository` double.

TST-NOT-05 (implemented): Collaboration and system tests run with test properties, a test database with known readers and preferences, and recording `NotificationService` doubles or local receivers.

TST-NOT-06 (implemented): Mutation testing is applied to channel implementations, preference validation and dispatch; surviving mutants on the intersection and on the enabled-set check are reviewed.

## Motivation and trade-offs

Recording doubles make the observable result of a notification (channel, reader, content) directly assertable and guarantee that no real message is sent. Testing dispatch inside `LendingServiceImpl` with one double per channel checks the intersection rule without any real mechanism. Isolation improves control and fault localisation; collaboration tests then reveal wiring, composition and persistence errors that doubles hide.

The cost is fixtures and doubles to maintain, and channel implementations that are only partly exercised in class tests because their delivery stays inside the class. Tests can become brittle if they assert private implementation details or every incidental call.

## Alternatives

- **A — Replace the channels with a recording double of `NotificationService`:** consumers depend only on `NotificationService`; tests inject a double that records reader, channel and content. No new production element, follows EXT-NOT-03 and sends 0 real messages; the real channel implementations need their own tests.
- **B — Replace only the delivery client inside each channel:** executes the real channel, but ties every consumer test to real channel classes. Its technique is used only in channel class tests, for framework clients (see D).
- **C — Use real providers with a test account:** exercises real delivery, but violates 0 real messages (QAS10) and is slow, non-repeatable and dependent on external services.
- **D — Non-sending unit tests plus actual integration tests (selected):** The default suite and CI follow A and send nothing. Separate integration tests run in the pipelines against Mailpit (SMTP) and Mockoon (HTTP); real SMS delivery is a manual demo. The automated suite keeps 0 real messages and repeatable results, and delivery over SMTP and HTTP is also exercised. Mockoon only shows that a request reached the expected address, not that an SMS arrived. The pipeline machine needs Node and Mailpit, and the tests must be maintained separately.

D is the selected design: it mixes alternative A's approach, which describes QAS10 (channels replaced by doubles that record calls) and lets tests assert which channels were used, for which reader and with what content, while also permitting the testing of actually functioning mechanisms. The cost is that the channel implementations are not exercised by these tests and are verified separately. Channels keep their delivery inside the class (EXT-NOT-02, CON-NOT-5);

## Pending issues
None. 

Resolved during implementation:
- How channel class tests control delivery: the framework client the channel receives through its constructor (a mock `JavaMailSender`, and a `MockRestServiceServer` bound to the `RestClient.Builder`); local receivers at collaboration level;
- Schedule of the due-date and overdue check: daily at 09:00 by default, set at setup;
- Webhook target: the reader's URL, stored in `ReaderDetails`;
- Channel failure behaviour: logged, with no retry; the other channels and readers continue. An empty channel list stops startup;
- Fixtures for readers and preferences are built directly in each test; system tests run through HTTP with real tokens.

## Verification after implementation

Results (2026-10-30): 101 tests in the `notification` profile, 0 failures, 0 real messages. All 10 branches of the check and dispatch in `LendingServiceImpl` are covered, against the 80% that QAS10 requires, separate from the reported 85% overall goal. PIT: 69 mutations, 66 killed, 0 surviving; the 3 without coverage are in `LendingServiceImpl` code unrelated to notifications. See [commands, SUT boundaries and limitations](../../NotificationImplementation.md).