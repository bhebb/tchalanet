# Spec — ValidateOfflineSubmissionCommandHandler

## Responsibility

Run technical validation before a submission can be promoted.

## Checks

- hash/signature valid;
- device/offline grant valid;
- payload timestamp sane;
- sequence monotonic or duplicate-safe;
- tenant/terminal/outlet/session ids structurally valid;
- no replay detected.

## Output

- mark `VALIDATED_TECHNICALLY`, or
- mark `REJECTED`/`REQUIRES_REVIEW` with reasons.
