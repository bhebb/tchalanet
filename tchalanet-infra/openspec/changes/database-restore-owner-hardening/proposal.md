# Database restore ownership hardening

## Why

The application database backup is intentionally created and restored with
`--no-owner`. After a restore, the four analytics projection tables can
therefore be owned by the database administrator instead of `app_user`. The
API then sees empty tenant projections or fails on writes because this RLS
configuration relies on the application role owning those tables.

## What

- Normalize ownership of the four analytics projections before a remote backup
  is taken.
- Repeat the normalization after every remote restore and verify the result.
- Document that both guards are required because `--no-owner` remains part of
  the portability contract.

## Non-goals

- Changing tenant RLS policies or disabling RLS.
- Changing backup retention, encryption, or R2 credentials.
- Deleting or rebuilding application data during backup or restore.
