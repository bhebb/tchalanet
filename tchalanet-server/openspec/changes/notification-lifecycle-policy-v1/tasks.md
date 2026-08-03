# Tasks - notification lifecycle policy v1

## Contract

- [x] Map draw, draw-result and ticket events to their existing notification behavior.
- [x] Define recipients, scope, channel and no-noise rules for normal and exceptional paths.
- [x] Add the platform-notification specification delta.
- [x] Link the draw and sales domain lifecycle documents to the policy.

## Follow-up implementation

- [x] Add an aggregated settlement/payout attention event and notification after the threshold is defined.
- [x] Request WEB + Slack delivery for the settlement attention notice and cover creation/resolution in focused tests.
- [ ] Deploy and validate one delayed-settlement notification in STG (platform WEB + Slack, then automatic expiry after settlement or correction).
- [ ] Add end-to-end tests proving tenant owner/admin receive identical tenant notices and superadmin does not receive tenant-scoped notices.
