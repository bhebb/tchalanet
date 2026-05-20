# Specification Delta — platform.communication

## ADDED Requirements

### Requirement: Communication platform sends ticket proof as text in V1

`platform.communication` SHALL send the text body provided by the caller and SHALL NOT require a PDF attachment for ticket proof in V1.

#### Scenario: SMS send contains text only

- **GIVEN** cashier send request for SMS
- **WHEN** `CommunicationApi.enqueue` is called
- **THEN** the message SHALL contain a text body
- **AND** SHALL NOT require document storage, signed file URLs, or PDF attachments

### Requirement: Communication deduplicates ticket sends within 60 seconds

`platform.communication` SHALL deduplicate ticket receipt sends by `(ticketId, channel, recipient)` within 60 seconds.

#### Scenario: Duplicate send returns deduplicated

- **GIVEN** a message for ticket T, channel WHATSAPP, recipient R was enqueued less than 60 seconds ago
- **WHEN** the same ticket/channel/recipient send is requested again
- **THEN** no new outbound message SHALL be enqueued
- **AND** the caller SHALL receive `deduplicated = true`

### Requirement: Communication dedup metadata is explicit

Ticket receipt send metadata SHALL include stable fields required for deduplication.

Required fields:

```text
ticketId
publicCode
channel
recipient
dedupKey
```

#### Scenario: Missing dedup metadata rejected or treated as non-dedupable

- **GIVEN** a ticket receipt send request without required dedup metadata
- **WHEN** communication enqueue is attempted
- **THEN** the system SHALL either reject the request or treat it as non-dedupable according to communication policy
- **AND** the caller code SHALL provide the metadata in normal cashier flows
