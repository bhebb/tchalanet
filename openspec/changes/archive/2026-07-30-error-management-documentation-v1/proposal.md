# Error management documentation v1

## Why

The implementation contract is now shared across backend, web, and mobile, but the rules are
spread across feature docs and code. Contributors need one functional explanation of blocking
errors, non-blocking degradation, ownership, localization, correlation, and retry safety.

## Scope

- Update the web `error-management.md` convention with the cross-project map and current core
  responsibilities.
- Add normative backend and mobile core error-management conventions.
- Link the guides from each project documentation index.

## What Changes

- Add a cross-project error-management guide for the backend, web, and mobile core boundaries.
- Update the existing web convention and API documentation indexes with the canonical links.
- Record the documentation contract in the root OpenSpec feature specifications.

## Out of scope

- Changing runtime behavior or API payloads.
- Adding new error codes or changing retry policies.
