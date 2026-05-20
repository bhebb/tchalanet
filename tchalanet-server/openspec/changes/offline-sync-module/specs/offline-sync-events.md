# Spec — Offline Sync Events

## Public events

Only expose events in `core.offlinesync.api.event` when other modules legitimately consume them.

Candidate events:

- `OfflineSubmissionReceivedEvent`
- `OfflineSubmissionRejectedEvent`
- `OfflineSubmissionPromotedEvent`
- `OfflineSubmissionRequiresReviewEvent`

## Rules

- Events carry tenant id and submission id.
- Events do not carry full sensitive payload by default.
- Events are published after commit.
- Target modules execute their own commands/services.
