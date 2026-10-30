# Technical Memo — CFG-NOT: Notification configurability

To see the implementation: [implemented contracts and verification](../../NotificationImplementation.md). The decisions below describe the implementation; the report records contracts and evidence.

Related iteration: [Configurability, iteration 2](../../Configurability.md). Primary drivers: QAS5 (setup-time channel selection) and QAS6 (reader preferences at runtime). Related drivers: QAS2, QAS10. Related requirements: US8, US9. Faculty basis: Project 1 section 1.3.3.2 and the Technical Memo examples in *Something about Attribute-Driven Design 0.9* (PDF pages 56 and 85-87).

## Issue

The current code has no notification module and no configuration for notification channels. The Administrator must define at setup which channels are enabled, without changing the source code, and each Reader must be able to choose one or several of the enabled channels.

## Problem

How can the enabled notification channels be selected at setup, and reader preferences be restricted to them, without the lending service reading configuration or naming channels?

## Summary of solution

A setup property lists the enabled channel identifiers. At startup a composition element registers only the matching `NotificationService` implementations and injects them into `LendingServiceImpl`. Reader preferences are validated against the enabled set and stored per reader in `ReaderDetails`; notifications go through the intersection of enabled and chosen channels.

## Tactics

- Configuration-Driven Behavior: the enabled channels are defined in a project-level setup property, not in code.
- Defer Binding: the enabled implementations are composed at startup; a change takes effect after one restart.
- Use an Intermediary: the composition element stands between the configuration and `LendingServiceImpl`, so the service reads no notification properties.
- Separation of Concerns / Restrict Dependencies: reading configuration, validating it and using it are separate responsibilities; `LendingServiceImpl` depends only on `NotificationService` and the injected channels.

Supporting concepts selected in Step 4: Dynamic Binding for reader preferences (runtime data restricted to the channels bound at setup), and Dependency Injection, Isolation and Mocking (testability, detailed in [Testability — Notification methods](../Testability/NotificationMethods.md)).

## Factors

- Selection of the enabled channels happens at setup time: 0 source changes, no recompilation, effective after one restart (QAS5, CON-CFG-1, CON-CFG-2).
- Reader preferences are runtime data, applied without restart and limited to the enabled channels (QAS6, CON-CFG-3).
- The channel contract and implementations are agreed in [Extensibility — Notification Methods](../Extensibility/NotificationMethods.md) (EXT-NOT-01 to EXT-NOT-05).
- The Administrator decides at setup which channels are available (US9). Each reader then chooses among those available channels (US8), and that choice is saved in `ReaderDetails`.

## Solution

CFG-NOT-01 (implemented): A setup property lists the enabled notification channels by identifier. Only the matching `NotificationService` implementations are registered and injected into `LendingServiceImpl`; an empty list, an unknown or repeated identifier, or a missing delivery setting of an enabled channel stops startup.

CFG-NOT-02 (implemented): Reader notification preferences are validated against the enabled set and stored per reader in `ReaderDetails`. A reader must choose at least one channel; an empty choice is rejected, and a choice that includes a disabled channel is rejected with an error naming the channel.

CFG-NOT-03 (implemented): Dispatch, in `LendingServiceImpl`, uses the intersection of enabled channels and the reader's chosen channels.

CFG-NOT-04 (implemented): the schedule of the periodic check (daily at 09:00 by default) and the notice day (3 days before the limit date by default) are setup values, bound by the channel composition. The schedule can be disabled; a malformed schedule or a notice day that is not above 0 stops startup.

## Motivation and trade-offs

Selecting channels at setup lets each installation use only the mechanisms it needs, without code changes, and disabled channels send 0 notifications. Readers choose among the enabled channels at runtime, and one reader's choice does not affect another. The cost is a composition element, an identifier convention and a restart for every change to the enabled set. Disabling a channel can leave related `ReaderDetails` data (for example the phone number when SMS is disabled) unused. As currently specified, the enabled channels cannot be changed after setup without editing the configuration and restarting.

## Alternatives

- **Channel names read inside the lending service:** few types, but adding a channel changes dispatch logic, increasing extension impact (QAS2).
- **Identifier list bound to injected implementations (selected):** the lending service stays free of channel names and properties, is testable and consistent with the extension design.
- **Enable flags stored in the database:** no restart, but the requirement is setup-time selection; adds persistence and authorization concerns.

See Step 4 of [Configurability.md](../../Configurability.md), iteration 2.

## Pending issues
None. Resolved during implementation:

- Channel list settings: resolved in the implementation.
- Behaviour when no channel is enabled (empty list): startup stops, because at least one channel must be enabled (group decision).
- Stored preferences that include a channel disabled at a later setup: the channel is skipped by the intersection (CFG-NOT-03).
- Source of the webhook target: each reader provides their own URL, stored in `ReaderDetails`.

## Verification
Results of Step 7 of the linked iteration (Analysis and next verification — Iterations 2 and 3):

- Combinations of channels (single, several) are verified at configuration level, with no code change; no whole-application restart experiment was run.
- A choice including a disabled channel is rejected naming the channel; an empty choice is rejected; other readers' preferences are unaffected.

101 notification tests pass. See [commands, SUT boundaries and limitations](../../NotificationImplementation.md).