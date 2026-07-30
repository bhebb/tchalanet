# Subscription admin resilience v1

## Goal

Make the tenant subscription console consistent with the web error ownership contract and
the admin i18n conventions.

## Decisions

- Loading the subscription remains a page-owned failure because the page has no meaningful
  subscription content without that response.
- Renew, cancel, suspend, and resume failures remain owned by the Actions section and are
  normalized as section errors.
- Add local fallback copy for the page and dialogs in `en`, `fr`, and `ht`.

## Non-goals

- Changing subscription state transitions or API contracts.
- Changing plan entitlement or billing semantics.
