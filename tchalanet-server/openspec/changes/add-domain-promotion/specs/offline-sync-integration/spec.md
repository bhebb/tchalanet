# Specification: offline-sync-integration

## ADDED Requirements

### Requirement: Promotions are not applied offline by default

The offline sales flow SHALL not apply promotions unless the rule explicitly allows offline execution.

#### Scenario: Online-only free Maryaj rule while offline

- GIVEN the device is offline
- AND a free Maryaj rule has `offline_allowed = false`
- WHEN offline preview/sale is attempted
- THEN the promotion SHALL not be granted.

### Requirement: Offline promotion support requires signed snapshots later

Future offline promotion support SHALL use signed rule snapshots and server revalidation.

#### Scenario: Offline rule snapshot expired

- GIVEN a device uses an expired signed rule snapshot
- WHEN it syncs an offline sale
- THEN the server SHALL reject or mark the submission for review.
