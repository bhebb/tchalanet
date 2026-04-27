# Proposal: Enforce Draw Result Finality Rules

**Date**: 2026-04-26  
**Status**: COMPLETED / ARCHIVED  
**Author**: Agent

## 1. Context & Goals

Following the pipeline audit, we identified risks related to real money settlement. This change enforces defensive business rules:

- Settle is only allowed on `FINAL` results.
- Override is strictly forbidden if a draw is already `SETTLED`.
- Usage of `force=true` flag requires a mandatory reason and is systematically audited.

## 2. Changes

### 2.1 Business Rules

- `SettleDrawsCommandHandler`: Checks for `FINAL` status.
- `OverrideDrawResultCommandHandler`: Checks for `SETTLED` draws via a new `DrawReaderPort`.
- Automatic re-apply of results to non-settled draws after a valid override.

### 2.2 Security & Compliance

- Aspect `@AuditedForceCommand` for tracking administrative bypasses.
- Mandatory `reason` field on all commands using the `force` flag.
- Documentation of conventions in `ops_force_flag.md`.

### 2.3 Monitoring

- `DrawProvisionalWatchdogScheduler`: Alerts on draws stuck in `PROVISIONAL` state for more than 30 minutes.
