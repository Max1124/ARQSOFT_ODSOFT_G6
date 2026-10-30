# Notification implementation and verification

Report date: 2026-10-30. Scope: reader notifications in ADD.md (US6, US7, US8, US9; QAS2, QAS5, QAS6, QAS10).

## What was implemented and why

- `NotificationService` is the extension contract: `channel()` gives the identifier and `send(readerNumber, notification)` delivers. `EmailNotificationService`, `SMSNotificationService` and `WebhookNotificationService` each keep their own protocol, message format and delivery client. A new mechanism implements the contract and is registered at setup; a fake fourth channel exercises this extension.
- `LendingNotification` is the event sent to a channel: due date approaching, or overdue with its days overdue and fine. It is plain data, so channels and their tests do not depend on the `Lending` entity.
- `NotificationChannelsConfig` is the setup composition. It registers the installed channels (identifier and supplier) and binds the schedule and notice day, so `LendingServiceImpl` reads no property. `NotificationChannelComposition` turns the configured identifiers into the enabled channels, in order; only the listed channels are created, so disabled channels send 0 notifications.
- `LendingServiceImpl.checkDueAndOverdueLendings` is the periodic check. It reads the outstanding lendings, builds the event with the injected `Clock`, and dispatches through the intersection of enabled and chosen channels, read at each check.
- `NotificationPreferenceService` validates a reader's choice against the enabled set and stores it in `ReaderDetails`. A choice must have at least one channel, and a disabled channel is rejected, naming it. The choice is runtime data and applies to the next notification.
- `WebhookUrl` holds the target of the webhook channel: an absolute http or https URL provided by the reader and stored in `ReaderDetails`. Choosing the webhook channel requires a URL; without one in a later request, the stored URL is kept.

## Decisions settled in the implementation

- **Channel failure:** a channel reports a failure by throwing. `LendingServiceImpl` logs it and continues with the other channels and readers. There is no retry.
- **Notice frequency:** an overdue notice is sent on every check while the lending is overdue, so the reader sees the fine grow. The due-date notice is sent on every check on the notice day: once with the default daily schedule, and repeated with a more frequent one.
- **No channel enabled:** an empty channel list stops startup, because at least one channel must be enabled. So do an unknown or repeated identifier and a missing delivery setting of an enabled channel. Disabled channels need no settings.
- **Webhook target:** each reader provides their own URL, so the channel has no setup value.
- **Check schedule:** daily at 09:00 by default, set at setup. It can be disabled; a malformed schedule stops startup. It is bound in `NotificationChannelsConfig`, not in the service.
- **Notice day:** 3 days before the limit date by default, set at setup and validated by `NotificationParameters`; a value that is not above 0 stops startup.
- **Later-disabled channel:** a stored channel that is no longer enabled is skipped by the intersection.
- **Reader with no choice:** a reader who has never chosen a channel receives no notifications; there is no default channel.
- **Controlling delivery in tests:** class tests control the framework client each channel uses: a mock `JavaMailSender`, and a `MockRestServiceServer` bound to the `RestClient.Builder`.
- **Test execution:** all automated tests run under Surefire; the `*IT` classes are not selected by the default test selection.

## REST contract

- `PUT /api/readers/notification-channels`, role READER: JSON `{ "channels": ["email", "webhook"], "webhookUrl": "https://example.com/hook" }`; `webhookUrl` is optional. It changes only the authenticated reader's own choice. Returns 200 with `{ "channels": [...] }`.
- 400 for an empty choice, a channel that is not enabled (the message names it), the webhook channel without any URL, or an invalid URL; nothing is stored. Authentication and role failures: 401 and 403.

There is no endpoint to read the enabled channels or the reader's current choice.

## Reproduction

```powershell
.\mvnw.cmd -Pnotification test
.\mvnw.cmd -Pnotification test-compile org.pitest:pitest-maven:mutationCoverage
.\mvnw.cmd test
```

The Jenkins pipeline runs the first two in the `Notification tests` and `Notification mutation testing` stages. The integration tests against the actual mechanisms are a separate manual step, listed at the [end of Testability.md](Testability.md#post-implementation-addition--integration-tests).

## Test boundaries and evidence

- Class contracts: `LendingServiceImplNotificationTest` (recording channels, repository doubles, fixed clock); `EmailNotificationServiceTest`, `SMSNotificationServiceTest`, `WebhookNotificationServiceTest` (repository double, controlled framework client); `NotificationChannelCompositionTest`, `NotificationParametersTest`, `NotificationPreferenceServiceTest`.
- Domain transparent-box: `ReaderDetailsNotificationChannelsTest`, covering the choice rules of `ReaderDetails` and `WebhookUrl`.
- Setup composition: `NotificationChannelsConfigTest` starts only the notification configuration with test properties.
- Extension experiment: `FourthNotificationChannelExtensionTest` runs a fake fourth channel with the real composition, preference validation, check, dispatch and existing channels. It is a collaboration test, not single-class isolation. Changed files: one new test class; no production class changed. A real installation also adds its class and one registration entry in `NotificationChannelsConfig`.
- Component: `NotificationApiComponentTest` with the real controller, preference validation, check, dispatch, email and SMS channels, repositories and database. Email and SMS deliver to local receivers; the webhook is disabled; authentication is simulated.
- System: `NotificationSystemTest` through HTTP with real login and JWT, all three channels delivering to local receivers on the loopback address. The test runs the periodic check directly.
- Mutation: limited to the notification services, `NotificationPreferenceService` and `LendingServiceImpl`. Six unrelated `LendingServiceImpl` methods are excluded (`listByReaderNumberAndIsbn`, `setReturned`, `getAverageDuration`, `getOverdue`, `getAvgLendingDurationByIsbn`, `searchLendings`); `create`, `renew` and `findByLendingNumber` are still mutated. Not a claim for the whole application.

### Recorded results — 2026-10-30

- Notification profile: **101 tests, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS.
- Full regression (`./mvnw test`): **682 tests, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS.
- JaCoCo, `notification` profile scope: **154/154 lines**, **46/48 branches (95.83%)**. This scope does not include `LendingServiceImpl`.
- JaCoCo, check and dispatch methods in `LendingServiceImpl`, from a report over all classes using the same execution data: **10/10 branches**. This meets the 80% QAS10 target.
- PIT: **69 generated, 66 killed, 0 survived, 3 without coverage**, test strength 100%. The 3 without coverage are in `LendingServiceImpl` code unrelated to notifications: `findByLendingNumber` and two not-found lambdas.
- 0 real messages: the default suite and the notification profile select only `*Test` classes, which deliver to mocks and to local receivers on the loopback address.

Machine-readable summary: [NotificationValidation.json](Evidence/NotificationValidation.json).

### Limits of the evidence

- The integration tests against Mailpit, the SMS gateway and Mockoon are not part of these results.
- The scheduler firing is not part of these results. `ScheduledNotificationCheckIT` covers it as a manual test against Mailpit; the automated tests check that the schedule is registered and call the check directly.
- A restart with a changed value is covered at configuration level; no whole-application restart experiment was run.
- Collaboration and system tests use the real clock with dates relative to today.
- With all three channels enabled, an installation must provide `NOTIFICATION_SMS_USERNAME` and `NOTIFICATION_SMS_PASSWORD`, or startup stops. Each deployed environment sets them in `/etc/lms/<env>.env`.