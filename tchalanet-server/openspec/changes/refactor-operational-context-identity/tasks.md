# Tasks — Refactor Operational Context and Identity

## 1. Rename usercontext decision

- [ ] Remove references to `platform.usercontext` from docs/specs.
- [ ] Replace with `platform.identity`.
- [ ] Add glossary entries for request context, identity, access control, and operational context.

## 2. Split packages

- [ ] Keep `TchRequestContext`, `TchContext`, binders/resolvers in `common.context`.
- [ ] Keep MVC argument resolver annotations in `common.web.context`.
- [ ] Move user/profile/membership/bootstrap code to `platform.identity`.
- [ ] Ensure `platform.identity` does not import `common.web.context` except through web controllers.

## 3. Add API

- [ ] Create `platform.identity.api.IdentityApi`.
- [ ] Create API models under `platform.identity.api.model.request/view/result`.
- [ ] Keep controllers under `platform.identity.internal.web`.
- [ ] Keep JPA under `platform.identity.internal.persistence`.

## 4. Operational context resolver

- [ ] Create `ResolveSellerOperationalContextRequest` model.
- [ ] Create `SellerOperationalContextView` or result model.
- [ ] Implement resolver initially where first used, or under `platform.operationalcontext` if shared.
- [ ] Ensure resolver uses APIs only, not internal packages.

## 5. Guards/tests

- [ ] ArchUnit: no `platform.usercontext..` package.
- [ ] ArchUnit: `common.context` does not depend on `platform..`.
- [ ] ArchUnit: `platform.identity` does not depend on `core.sales..`.
- [ ] Add tests for seller operational context happy path and failure cases.
