# Technical Memo — CFG-NOT: Notification configurability

Status: proposed architecture; not implemented or verified.

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
- Use an Intermediary: the composition element stands between the configuration and `LendingServiceImpl`, so the service never reads properties.
- Separation of Concerns / Restrict Dependencies: reading configuration, validating it and using it are separate responsibilities; `LendingServiceImpl` depends only on `NotificationService` and the injected channels.

Supporting concepts selected in Step 4: Dynamic Binding for reader preferences (runtime data restricted to the channels bound at setup), and Dependency Injection, Isolation and Mocking (testability, detailed in [Testability — Notification methods](../Testability/NotificationMethods.md)).

## Factors

- Selection of the enabled channels happens at setup time: 0 source changes, no recompilation, effective after one restart (QAS5, CON-CFG-1, CON-CFG-2).
- Reader preferences are runtime data, applied without restart and limited to the enabled channels (QAS6, CON-CFG-3).
- The channel contract and implementations are agreed in [Extensibility — Notification Methods](../Extensibility/NotificationMethods.md) (EXT-NOT-01 to EXT-NOT-05).
- The Administrator decides at setup which channels are available (US9). Each reader then chooses among those available channels (US8), and that choice is saved in `ReaderDetails`.

## Solution

CFG-NOT-01 (proposed): A setup property lists the enabled notification channels by identifier. Only the matching `NotificationService` implementations are registered and injected into `LendingServiceImpl`; an unknown identifier stops startup.

CFG-NOT-02 (proposed): Reader notification preferences are validated against the enabled set and stored per reader in `ReaderDetails`. A reader must choose at least one channel; an empty choice is rejected, and a choice that includes a disabled channel is rejected with an error naming the channel.

CFG-NOT-03 (proposed): Dispatch, in `LendingServiceImpl`, uses the intersection of enabled channels and the reader's chosen channels.

## Motivation and trade-offs

Selecting channels at setup lets each installation use only the mechanisms it needs, without code changes, and disabled channels send 0 notifications. Readers choose among the enabled channels at runtime, and one reader's choice does not affect another. The cost is a composition element, an identifier convention and a restart for every change to the enabled set. Disabling a channel can leave related `ReaderDetails` data (for example the phone number when SMS is disabled) unused. As currently specified, the enabled channels cannot be changed after setup without editing the configuration and restarting.

## Alternatives

- **Channel names read inside the lending service:** few types, but adding a channel changes dispatch logic, increasing extension impact (QAS2).
- **Identifier list bound to injected implementations (selected):** the lending service stays free of channel names and properties, is testable and consistent with the extension design.
- **Enable flags stored in the database:** no restart, but the requirement is setup-time selection; adds persistence and authorization concerns.

See Step 4 of [Configurability.md](../../Configurability.md), iteration 2.

## Pending issues

- Property name, file location and environment-variable precedence for the channel list.
- Behaviour when no channel is enabled (empty list).
- Stored preferences that include a channel disabled at a later setup (for example a reader whose only chosen channel is no longer enabled).
- Source of the webhook target.

## Verification
Follow Step 7 of the linked iteration (Analysis and next verification — Iterations 2 and 3):

- Enable each combination of channels (single, several) and confirm the effect after one restart with no code change.
- Change a reader's channels to include a disabled one and confirm rejection naming the channel; submit an empty choice and confirm rejection; confirm other readers' preferences are unaffected.

No implementation or passing results are claimed yet.