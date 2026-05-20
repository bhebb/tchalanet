# Change: refactor-common-batch-alert-event

## Why

The previous batch Slack notification mechanism broke after module restructuring because it called an external communication gateway directly from a generic/common batch concern. This violates dependency direction and provider encapsulation.

## What changes

- Keep generic batch annotation/aspect in `common.batch.alert`.
- Make it publish provider-neutral `BatchFailedEvent`.
- Move Slack/email delivery decisions to `platform.communication`.
- Move in-app ops notification decisions to `platform.notification`.

## Impact

- Remove direct Slack gateway imports from common/batch/schedulers.
- Add common system event.
- Add platform listeners/rules.
