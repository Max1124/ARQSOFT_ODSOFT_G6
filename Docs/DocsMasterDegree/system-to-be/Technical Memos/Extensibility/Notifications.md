# Technical Memo — EXT-BIB-01: Notification extensibility

Status: proposed architecture; not implemented or verified.

Related iteration: [Extensibility, iteration 1](../../Extensibility.md). Drivers: US6, US7, US8/QAS2. Faculty basis: Project 1 section 1.3.3.1 and the Technical Memo examples in *Something about Attribute-Driven Design 0.9* (PDF pages 56 and 85-87).

## Issue

The current notification module has no capacity to add new notification methods. The project must allow new methods to notify readers without impacting existing functionality.

## Problem

How can the backend add notification methods with different mechanisms without repeatedly modifying notification business logic?

## Summary of solution

Introduce a notification service interface that is injected to an instance related to a specific notification mechanisms.

## Tactics

- Abstract common methods: one interface describes the functionalities common to all notification methods.
- Encapsulate change: isolate specific mechanisms in specific instantiations.
- Defer binding: select notification methods at setup; configuration details remain to be refined within bibliographic iteration 1 and documented under configurability.
- Restrict dependencies and separate concerns: keep provider DTOs/HTTP outside the domain and separate retrieval, mapping, merge and persistence.

Supporting concepts selected in Step 4: Adapter and Data Transformation (interoperability), Configuration-Driven Behavior and Use of Abstractions (flexibility), and Dependency Injection, Isolation and Mocking (testability). Overlapping notification terms describe the same choices; they do not require extra components.

## Factors

- Multiple notification methods are required.
- Tests need controlled external responses and replaceable dependencies.
- Preserve the existing layered Java/Spring REST backend.

## Solution

EXT-BIB-01 (proposed): `BibliographicSource` defines ISBN lookup; Google/Open Library adapters normalize responses into `BibliographicData`; `BibliographicService` coordinates the selected sources; isolated merge logic combines results. Source outcomes distinguish no match from communication failure.

EXT-BIB-02 (agreed with the group): associate existing authors/genre automatically only when matching is unambiguous under defined rules. The librarian completes missing/ambiguous data and explicitly creates missing catalogue records when appropriate. Retrieval itself creates no authors/genres.

EXT-BIB-03 (proposed): isolate normalized partial metadata and deterministic merge from HTTP and persistence. Source precedence, conflicts and list handling remain pending.

EXT-BIB-04 (proposed): compose the ordered enabled source collection at setup and inject it into the coordinator. Configuration, partial-result composition and test isolation belong to roadmap iteration 1; detailed configuration remains to be refined.

## Motivation and trade-offs

New notification methods can be implemented can be implemented with the same structure as previous ones while retaining their specific mechanisms. The cost is a specific implementation of each notification method.
## Alternatives

Method-specific calls inside one single service need less initial structure but require changing the service for every new method and might limit functionality of specific ways to notify the user. The interface alternative is selected as the current proposal because it addresses QAS2 in a manner that allows for better extensibility.

## Pending issues

- Implementing the notification module from scratch.
- Determining what notification methods to use for now.
- Establishing failure semantics that are compatible with all notification methods.
- Actual extension impact and regression results after implementation.

## Verification

Step 7 of the linked iteration does not address the implementation of the features described in this document. As such no verification is possible.