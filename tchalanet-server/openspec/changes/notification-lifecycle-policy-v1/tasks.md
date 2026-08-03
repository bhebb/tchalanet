# Tasks - notification lifecycle policy v1

## Contract

- [x] Map draw, draw-result and ticket events to their existing notification behavior.
- [x] Define recipients, scope, channel and no-noise rules for normal and exceptional paths.
- [x] Add the platform-notification specification delta.
- [x] Link the draw and sales domain lifecycle documents to the policy.

## Follow-up implementation

- [ ] Add an aggregated settlement/payout attention event and notification after the retry threshold is defined.
- [ ] Add communication-channel mapping and integration tests for the settlement attention notice.
- [ ] Add end-to-end tests proving tenant owner/admin receive identical tenant notices and superadmin does not receive tenant-scoped notices.
