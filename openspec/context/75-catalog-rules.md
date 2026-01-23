# Conventions — Catalogs (CRUD & Exposure)

## Goal

Catalogs are **reference / lookup** domains:

- read-mostly
- small or bounded datasets
- stable contracts for other layers
- minimal business logic (ideally none)

Catalogs MUST remain safe to consume and MUST NOT leak internal persistence.

---

## 1) Spring Data REST is forbidden

**Spring Data REST (SDR)** MUST NOT be used for catalogs.

### Forbidden

- `@RepositoryRestResource`
- `@RestResource`
- exposing JPA entities directly through SDR endpoints

### Rationale

- leaks internal JPA structure (`internal.infra.persistence.*`)
- bypasses validation, authorization, and cache eviction rules
- makes API evolution and security harder
- creates hidden surface area (endpoints you did not design)

**Conclusion**: DO NOT use Spring Data REST for the Catalogs.

---

## 2) Read contract (public API)

Each catalog MUST expose a **read-only contract** under the package:

```
com.tchalanet.server.catalog.<name>.api
```

Example:

- `ResultSlotCatalog`
- `ResultSlotView`

Rules:

- methods are **side-effect free**
- methods MUST NOT emit domain events
- methods MUST NOT perform orchestration
- DTOs are flat, immutable, cache-friendly
- internal details are not leaked (entities/ports/adapters remain internal)

Typical shape:

```java
public interface XCatalog {
  List<XView> listActive();
  Optional<XView> findByKey(String key);
  XView requireByKey(String key);
}
```

---

## 3) Internal persistence and implementation

Persistence belongs to internal packages, for example:

- `catalog.<name>.internal.infra.persistence`
- `catalog.<name>.internal.*`

Guidelines:

- `JpaRepository` implementations are allowed and remain internal-only.
- Repositories MUST NOT be exposed via SDR.
- Paging is not required unless the dataset is truly large (in which case an ADR is required).

---

## 4) Write side (admin CRUD)

Catalog writes MUST be implemented via a Controller (web boundary) + Service (write logic).

**NOT** via repository exposure (SDR).

Recommended packages:

- Controller: `catalog.<name>.internal.infra.web`
- Service: `catalog.<name>.internal.admin` (or `internal.application`)

Write rules:

- Validate input in controller/service (do not rely on DB errors for validation).
- Enforce authorization (e.g. `SUPER_ADMIN` or `TENANT_ADMIN` depending on catalog scope).
- Use soft-delete when applicable (`deleted_at`).
- Perform cache eviction on write handlers/services (create/update/delete).
- Admin endpoints MUST return the standard `ApiResponse<T>` wrapper for all 2xx responses.

Example pattern (simplified):

```java
@RestController
@RequestMapping("/platform/x")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
class XAdminController {
  private final XAdminService admin;

  @GetMapping("/active")
  public ApiResponse<List<XView>> listActive() {
    return ApiResponse.success(admin.listActive());
  }

  @PostMapping
  public ApiResponse<XView> create(@RequestBody @Valid CreateXRequest req) {
    var created = admin.create(req);
    return ApiResponse.created(mapper.toView(created));
  }
}
```

---

## 5) Cache policy

- Catalog reads SHOULD be cached (long TTL recommended).
- Recommended cache names:
  - `catalog:<name>:active`
  - `catalog:<name>:by_key`

Eviction:

- MUST happen on write handlers/services (create/update/delete).
- Read implementations MUST remain side-effect free.

Cache constants location:

- Constants MAY live under `catalog.<name>.internal.cache` to avoid cross-domain coupling.

---

## 6) Testing expectations

Minimum expected tests:

- Unit tests for read mapping and filtering (e.g. `listActive()` filters `active` and `deleted_at`).
- Unit or slice tests verifying soft-delete filtering behavior.
- Verify presence of `@CacheEvict` on write handlers (unit test or lightweight integration).
- ArchUnit rules to prevent dependencies on `internal.*` packages from outside the catalog.

---

## 7) Summary (non-negotiables)

- ✅ Catalog = reference data (read‑mostly)
- ✅ Public contract = `catalog.<name>.api`
- ✅ Writes via Controller + Service (not SDR)
- ✅ Repositories remain internal-only
- ✅ Read via cache + evict on writes
- ❌ No Spring Data REST
- ❌ No JPA entities exposed as HTTP responses

---

## Enforcement / follow-up

- Add ArchUnit rules and CI checks that:
  - prevent `@RepositoryRestResource` usage in `catalog.*`
  - detect controllers returning JPA types from `internal.infra.persistence`
- Document the controller/service pattern and examples in `tchalanet-server/docs/conventions/`.

---
