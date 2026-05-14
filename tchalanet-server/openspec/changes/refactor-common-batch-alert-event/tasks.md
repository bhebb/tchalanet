# Tasks

- [ ] Locate old batch Slack annotation/aspect/gateway usages.
- [ ] Move/replace annotation with `common.batch.alert.NotifyOnBatchFailure`.
- [ ] Add provider-neutral `BatchFailedEvent`.
- [ ] Add `BatchAlertSeverity`.
- [ ] Add aspect that publishes `BatchFailedEvent` only.
- [ ] Ensure common has no dependency on platform communication or Slack types.
- [ ] Add `BatchAlertNotificationListener` in `platform.notification`.
- [ ] Add `BatchAlertCommunicationListener` in `platform.communication`.
- [ ] Add ArchUnit rule banning Slack gateway imports outside platform communication internal adapter.
- [ ] Add tests for duplicate/correlation behavior.
