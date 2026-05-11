# Spec Delta — Sales Session Automation

## Requirement: Auto-open sales sessions when sellable draws open

The system SHALL support optional per-outlet automatic sales session opening after sellable draws open for a tenant business day.

### Scenario: auto-open enabled and no current session

Given an outlet has `auto_open_session=true`
And the outlet is not sales blocked
And at least one sellable draw is OPEN for the outlet business date
And an eligible seller has no OPEN SalesSession
When the auto-open command runs
Then the system creates a SalesSession for that seller
And marks its source as `SCHEDULER`.

### Scenario: seller already opened manually

Given a seller already has an OPEN SalesSession
When the auto-open command runs
Then the system does not create another session
And reports the seller as skipped with reason `session_already_open`.

## Requirement: Auto-close sales sessions after last sellable draw window

The system SHALL support optional per-outlet automatic sales session closing after no open or future sellable draws remain for the business day.

### Scenario: last draw closed

Given an outlet has `auto_close_session=true`
And no OPEN draws remain for the outlet business date
And no future sellable draws remain for the outlet business date
And no sale operation is in progress for the session
When the auto-close command runs
Then the system closes eligible OPEN SalesSessions
And marks closure source as `SCHEDULER`.

### Scenario: draw still open

Given at least one draw is still OPEN
When the auto-close command runs
Then the system does not close the SalesSession.

### Scenario: payout pending

Given no draw is open or future sellable
And a winning ticket payout is still pending
When the auto-close command runs
Then the system MAY close the selling SalesSession
Because payout may happen after selling session closure.
