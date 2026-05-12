# Spec — Core API Alignment

## Requirement: core public Java contracts live in api packages

Each core domain SHALL expose cross-module Java contracts through `core.<domain>.api`.

Allowed subpackages:

- `api.command`
- `api.query`
- `api.event`
- `api.model`

### Scenario: feature lists tickets

Given a feature needs ticket data  
When it asks for tickets  
Then it imports `core.sales.api.query.ListTicketsQuery` and `core.sales.api.model.TicketRow`  
And it does not import `core.sales.internal.*`.

## Requirement: core internals remain hidden

Handlers, aggregates, ports, repositories, controllers and adapters SHALL live under `core.<domain>.internal`.

### Scenario: another module imports Ticket aggregate

Given `Ticket` is an aggregate under `core.sales.internal.domain.model`  
When another module imports it  
Then ArchUnit/Modulith verification fails.

## Requirement: public events only in api.event

Only events intended for cross-module consumption SHALL live in `api.event`.

### Scenario: internal aggregate event

Given an event is used only inside the sales domain  
When it is not a cross-module contract  
Then it remains internal and is not placed in `api.event`.
