# Technical Memo — EXT-NOT: Notification extensibility

To see the implementation: [implemented contracts and verification](../../NotificationImplementation.md). The decisions below describe the implementation; the report records contracts and evidence.

Related iteration: [Extensibility, iteration 2](../../Extensibility.md). Primary driver: QAS2. Related drivers: QAS5, QAS6, QAS10. Related requirements: US6, US7, US8, US9. Faculty basis: Project 1 section 1.3.3.2 and the Technical Memo examples in *Something about Attribute-Driven Design 0.9* (PDF pages 56 and 85-87).

## Issue

The current code has no notification module. Readers must be notified by email, SMS and webhook, possibly through more than one channel at a time, and new notification channels must be added without impacting existing functionality.

## Problem

How can the backend add notification channels with different mechanisms without repeatedly modifying the lending workflow or the existing channels?

## Summary of solution

Introduce a `NotificationService` contract with one implementation per mechanism. The enabled implementations are composed at setup and injected into `LendingServiceImpl`, which notifies each reader through the channels that reader has chosen, using the contract only.

## Tactics

- Abstract Common Services / Use of Interfaces: one interface describes the behaviour common to all notification channels.
- Encapsulate: each implementation hides its own protocol, message format and delivery.
- Restrict Dependencies: `LendingServiceImpl` depends only on the contract; mechanism-specific clients stay inside the implementations, and contact data is read through `ReaderRepository`.
- Defer Binding: the enabled channels are composed at setup; configuration details are documented in [Configurability — Notification Methods](../Configurability/NotificationMethods.md).

Supporting concepts selected in Step 4: Configuration-Driven Behavior (flexibility), and Dependency Injection, Isolation and Mocking (testability, detailed in [Testability — Notification methods](../Testability/NotificationMethods.md)).

## Factors

- Three notification mechanisms are required now (email, SMS, webhook); more are expected.
- Notifications may use more than one channel at a time, and the channels in use are chosen at setup.
- Reader contact data already exists in the model: phone number in `ReaderDetails`, email as the `username` of `Reader`.
- Tests must not send real messages (QAS10), so the contract and the repository must be replaceable.

## Solution

EXT-NOT-01 (implemented): `NotificationService` defines the general behaviour of a notification mechanism; specific implementations of the interface detail the workings of specific mechanisms.

EXT-NOT-02 (implemented): `SMSNotificationService`, `EmailNotificationService` and `WebhookNotificationService` implement `NotificationService` for SMS, email and webhook delivery; each keeps its protocol and delivery details in its own class.

EXT-NOT-03 (implemented): `LendingServiceImpl` depends only on `NotificationService`, so adding a notification mechanism does not change `LendingServiceImpl`.

EXT-NOT-04 (implemented): Notification implementations obtain the reader's contact data through `ReaderRepository`, not its Spring Data implementation: the phone number and the webhook URL from `ReaderDetails` and the email from the `username` of its `Reader`.

EXT-NOT-05 (implemented): The enabled `NotificationService` implementations are composed at setup and injected into `LendingServiceImpl`, which notifies a reader through the injected channels that reader has chosen. Due-date and overdue notifications are triggered by a periodic, scheduler-driven check in `LendingServiceImpl`.

EXT-NOT-06 (implemented): a channel reports a delivery failure by throwing; `LendingServiceImpl` logs it and continues with the other channels and readers. There is no retry.

EXT-NOT-07 (implemented): an overdue notice is sent on every check while the lending is overdue; the due-date notice is sent on every check on the notice day, which is once with the default daily schedule.

## Motivation and trade-offs

A new channel is a new implementation of the same contract plus its registration at setup; the lending service and the existing channels stay unchanged, which is the QAS2 response. The cost is one implementation per mechanism and a shared contract that must suit all mechanisms, including their failure behaviour. The first introduction of the contract changes `LendingServiceImpl` once.

## Alternatives

- **Mechanism-specific calls in the lending service:** less initial structure, but every new channel changes `LendingServiceImpl`, mechanism dependencies enter business logic and real delivery is hard to replace in tests. Fails QAS2.
- **Common interface with one implementation per mechanism (selected):** isolates mechanism differences and lets channels be added or replaced without changing the lending service.

Trigger of due-date and overdue notifications:

- **T1 — Scheduled check in `LendingServiceImpl` (current proposal):** few types and reuses the lending repository and clock, but mixes scheduling with lending orchestration and fixes the schedule inside the service.
- **T2 — Reminders created with the lending:** notifications are planned per lending, but need extra persistence and must be updated or cancelled on renewals and returns.
- **T3 — Check on user activity:** needs no scheduler, but a reader who is not active is never warned, so it does not meet US6 and US7.

See Step 4 of [Extensibility.md](../../Extensibility.md), iteration 2.

Implementation note: T1 is implemented. The check stays in `LendingServiceImpl`, but its schedule is bound in `NotificationChannelsConfig`, so it is not fixed inside the service.


## Pending issues

- Further channels to add.

Resolved during implementation:
- Failure semantics compatible with all notification channels: EXT-NOT-06.
- Schedule of the periodic due-date and overdue check: daily at 09:00 by default, set at setup.
- Source of the webhook target: each reader provides their own URL, stored in `ReaderDetails`.


## Verification

Results of Step 7 of the linked iteration (Analysis — Iteration 2):

- Extension experiment: `FourthNotificationChannelExtensionTest` adds a fake fourth channel and its registration, enables it through the identifier list and selects it for a reader.
- `LendingServiceImpl` tests with recording doubles of `NotificationService`; channel tests with a replaced `ReaderRepository`; 0 real messages in the default suite and CI (QAS10).

101 notification tests pass. See [commands, SUT boundaries and limitations](../../NotificationImplementation.md).