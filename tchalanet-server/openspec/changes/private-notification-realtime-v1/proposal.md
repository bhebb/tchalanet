# Private notification realtime v1

## Why

The private notification bell currently refreshes only on page load or when the user opens it.
Published in-app notifications therefore remain invisible until an explicit pull.

## What changes

- Add authenticated SSE endpoints for the platform and tenant-admin notification scopes.
- Publish a content-free `notification-change` signal after a notification publication commits.
- Subscribe the private web shell once per authenticated session and refresh its existing notification snapshot on a signal.
- Keep REST list/count endpoints as the sole source of notification content and unread totals.

## Non-goals

- Browser/device push notifications.
- Persisting websocket/SSE delivery state.
- Replacing notification delivery policies or external communication channels.

## Validation

- Unit-test stream recipient routing and emitter cleanup.
- Unit-test the web store refreshes on a realtime signal and stops on teardown.
- Validate focused platform compilation/tests and web notification tests.
