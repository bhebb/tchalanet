# Proposal - notification external delivery V1

## Why

Platform alerts were recorded in-app but their automatic path did not request Slack delivery.
When a Slack policy was explicitly selected, the communication bridge targeted a `notifications`
key that edge-service does not configure. This makes an apparently successful notification unable
to reach the operations channel.

## What

- Let a notification rule declare its requested delivery channels.
- Keep tenant-domain automatic notifications in-app by default.
- Send automatic platform operations alerts to both in-app and Slack.
- Route internal Slack notifications to the configured edge-service `ops-alerts` webhook key.
- Cover the delivery policy and the outbound message mapping with focused tests.

## Non-goals

- No automatic tenant email or SMS fanout.
- No new communication provider or notification schema migration.
- No bypass of the durable `outbound_message` retry path.

