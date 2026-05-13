# Tasks — Refactor Operational Context and Identity

## 1. Rename usercontext decision

- [x] Remove references to `platform.usercontext` from docs/specs.
- [x] Replace with `platform.identity`.
- [x] Add glossary entries for request context, identity, access control, and operational context.

## 2. Split packages

- [x] Keep `TchRequestContext`, `TchContext`, binders/resolvers in `common.context`.
- [x] Keep MVC argument resolver annotations/wiring in the current context package.
- [x] Move user/profile/membership/bootstrap code to `platform.identity`.
- [x] Ensure `platform.identity` does not import `common.web.context` except through web controllers.

## 3. Add API

- [x] Create `platform.identity.api.IdentityApi`.
- [x] Create API models under `platform.identity.api.model.request/view/result`.
- [x] Keep controllers under `platform.identity.internal.web`.
- [x] Keep JPA under `platform.identity.internal.persistence`.

## 4. Operational context resolver

- [x] Create `ResolveSellerOperationalContextRequest` model.
- [x] Create `SellerOperationalContextView` or result model.
- [x] Implement resolver initially where first used, or under `platform.operationalcontext` if shared.
- [x] Ensure resolver uses APIs only, not internal packages.

## 5. Guards/tests

- [x] ArchUnit: no `platform.usercontext..` package.
- [x] ArchUnit: `common.context` does not depend on `platform..`.
- [x] ArchUnit: `platform.identity` does not depend on `core.sales..`.
- [x] Add tests for seller operational context happy path and failure cases.
