# Technical Memo — TST-NOT: Notification testability

Status: proposed design; no production or test implementation changed.

Related iteration: [Testability — Notification methods](../../Testability.md) (iteration 2). Primary driver: QAS10. Related: QAS2, QAS5, QAS6; US6, US7, US8, US9. Design under test: [Extensibility — Notification Methods](../Extensibility/NotificationMethods.md) (EXT-NOT-01 to EXT-NOT-05) and [Configurability — Notification Methods](../Configurability/NotificationMethods.md) (CFG-NOT-01 to CFG-NOT-03).

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
- Channel failure behaviour, the behaviour with no enabled channel, the webhook target and the schedule of the due-date and overdue check are not yet decided.

## Solution

TST-NOT-01 (proposed): Consumers of notifications depend only on `NotificationService`; tests inject a recording double that stores reader, channel and content.

TST-NOT-02 (proposed): Each channel implementation is tested as a single-class SUT with its `ReaderRepository` replaced and its delivery, which stays inside the class, controlled as decided in Step 4 of [Testability.md](../../Testability.md).

TST-NOT-03 (proposed): Channel composition is tested with test properties: only listed identifiers are registered; an unknown identifier fails startup; the empty value behaves as agreed.

TST-NOT-04 (proposed): Preference validation and dispatch are tested as separate class SUTs: an empty choice is rejected and a choice including a disabled channel is rejected naming it; dispatch, in `LendingServiceImpl`, uses the intersection of enabled and chosen channels and is tested with one recording `NotificationService` double per channel and a `ReaderRepository` double.

TST-NOT-05 (proposed): Collaboration and system tests run with test properties, a test database with known readers and preferences, and recording `NotificationService` doubles or local receivers.

TST-NOT-06 (proposed): Mutation testing is applied to channel implementations, preference validation and dispatch; surviving mutants on the intersection and on the enabled-set check are reviewed.

## Motivation and trade-offs

Recording doubles make the observable result of a notification (channel, reader, content) directly assertable and guarantee that no real message is sent. Testing dispatch inside `LendingServiceImpl` with one double per channel checks the intersection rule without any real mechanism. Isolation improves control and fault localisation; collaboration tests then reveal wiring, composition and persistence errors that doubles hide.

The cost is fixtures and doubles to maintain, and channel implementations that are only partly exercised in class tests because their delivery stays inside the class. Tests can become brittle if they assert private implementation details or every incidental call.

## Alternatives

- **A — Replace the channels with a recording double of `NotificationService` (selected):** consumers depend only on `NotificationService`; tests inject a double that records reader, channel and content. No new production element, follows EXT-NOT-03 and sends 0 real messages; the real channel implementations need their own tests.
- **B — Replace only the delivery client inside each channel:** executes the real channel, but needs one injectable delivery boundary per mechanism, which conflicts with keeping delivery inside each implementation, and ties every consumer test to real channel classes.
- **C — Use real providers with a test account:** exercises real delivery, but violates 0 real messages (QAS10) and is slow, non-repeatable and dependent on external services.

A is selected because it is the approach QAS10 describes and it requires no change to the extensibility design. See Step 4 of [Testability.md](../../Testability.md).

## Pending issues

- How channel class tests control delivery: the framework client the channel uses internally, or a local receiver at collaboration level;
- Schedule of the due-date and overdue check;
- Webhook target;
- Channel failure behaviour and behaviour with no enabled channel, including the empty channel list;
- Fixtures for readers and preferences, system setup and measurement/gate definitions.

## Verification after implementation

Collect test results, stability, coverage and mutation reports, and review surviving mutants. QAS10 requires at least 80% branch coverage on the dispatcher and channel selection logic, separate from the reported 85% overall goal. Each scenario must be checked by at least one automated test, with 0 real messages sent. No results are confirmed yet.
