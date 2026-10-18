# Technical Memo: Notification configurability

Status: proposed architecture; not implemented or verified.

Related iteration: [Configurability, iteration 1](../../Configurability.md). Drivers: QAS5, US8, US9 . Faculty basis: Project 1 section 1.3.3.1 and the Technical Memo examples in *Something about Attribute-Driven Design 0.9* (PDF pages 56 and 85-87).

## Issue

The current notification module has no capacity for the Admin to define at setup what notification methods will be available for a Reader to choose.

## Problem

How can the available notification methods be selected at set up and that choice be saved?

## Summary of solution

Introduce a functionality to define the available notification methods at setup and then also implement a mechanism that saves that choice.
## Tactics

- Config files: the available methods will be defined in a project-level configuration.
- Encapsulate change: isolate specific mechanisms in specific instantiations.
- Defer binding: select notification methods at setup; configuration details remain to be refined within bibliographic iteration 1 and documented under configurability.
- Restrict dependencies and separate concerns: keep the implementation and calling of responsibility restricted only to relevant classes.

Supporting concepts selected in Step 4: Data Transformation (interoperability), Configuration-Driven Behavior , Isolation and Mocking (testability).

## Factors

- Multiple notification methods are required.
- Tests need controlled external responses and replaceable dependencies.
- Preserve the existing layered Java/Spring REST backend.
- The choice of notification methods must be persisted.

## Solution

CFG-NOT-01: (proposed) A setup property lists the enabled notification channels by identifier. 
 
CFG-NOT-02 (proposed): Reader notification preferences are validated against the enabled set and stored per reader.
 
CFG-NOT-03 (proposed): Dispatch uses the intersection of enabled channels and the reader's chosen channels.


## Motivation and trade-offs

Allowing notification methods to be selected ar set up will allow for better interoperability, as LMS can adapt its notification mechanism to the needs of each client.
However, according to how requirements are currently specified, there is no functionality change notification methods past set up. Only using some notification methods might also cause some ReaderDetails parameters to be left unused.

## Alternatives

Instead of using a project level configuration file stored localy, the configuration could be stored in the database.

## Pending issues

- Implementing the config file.
- Implementing how to change the config file.
- Implementing storage of config file.

## Verification

Step 7 of the linked iteration does not address the implementation of the features described in this document. As such no verification is possible.