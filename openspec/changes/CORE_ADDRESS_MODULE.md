# core/address Module — Deduplication & Tenant-Scoped Architecture

## ✅ Completed: PR1 — Module Creation

### Structure

```
core/address/
├── domain/
│   ├── Address.java (immutable record)
│   ├── AddressNormalizer.java (normalize fields)
│   └── AddressDedupeKeyFactory.java (SHA-256 keying)
├── application/
│   ├── model/
│   │   ├── AddressInput.java (input DTO)
│   │   └── AddressView.java (read DTO)
│   ├── AddressCrudService.java (@UseCase, upsert + soft-delete)
│   └── port/
│       ├── AddressReaderPort.java (findById, findIdByNormalizedKey)
│       └── AddressWriterPort.java (save, upsert)
└── infra/persistence/
    ├── AddressJpaEntity.java (JPA entity + RLS)
    ├── AddressRepository.java (Spring Data)
    ├── AddressPersistenceAdapter.java (ports impl + dedup handling)
    └── mapper/
        └── AddressMapper.java (MapStruct)
```

### Deduplication Strategy

```
1. Normalize: line1, line2, city, region, country, postalCode
   - trim + lowercase
   - collapse multiple spaces
   - remove punctuation: , . - # ' "
   - postalCode: uppercase + remove spaces

2. Generate key: SHA-256(normalized_string) → 64-char hex

3. Upsert logic:
   a. Check findIdByNormalizedKey(tenantId, key)
   b. If found → return ID
   c. If not found → insert Address with key
   d. If race condition (DataIntegrityViolationException) → retry lookup → return ID

4. DB constraint: UNIQUE(tenant_id, normalized_key)
   - Ensures O(1) dedup via index lookup
   - Tenant-scoped (no global duplication)
```

### Domain Model

```java
public record Address(
    AddressId id,                  // typed ID
    TenantId tenantId,             // tenant scope
    String line1, line2,           // fields
    String city, region, country,
    String postalCode,
    String normalizedKey,          // SHA-256 hex
    boolean deleted,               // soft-delete
    Instant createdAt, updatedAt   // audit
)
```

### API (Ports Only — No Web Layer)

```java
// Reader
Optional<Address> findById(UUID tenantId, UUID addressId);
Optional<UUID> findIdByNormalizedKey(UUID tenantId, String normalizedKey);

// Writer
UUID save(Address address);
UUID upsert(UUID tenantId, String normalizedKey, Address address);
```

### CRUD Service

```java
UUID upsert(TenantId tenantId, AddressInput input)
  // → existing ID if dedup key found, new ID otherwise

Optional<AddressView> get(TenantId tenantId, UUID id)
  // → address view or empty

void softDelete(TenantId tenantId, UUID id)
  // → mark deleted = true
```

### Database

- **Table**: `address` (tenant-scoped, RLS enabled)
- **Unique constraint**: `(tenant_id, normalized_key)`
- **Indices**:
  - `idx_address_tenant_id` for tenant lookups
  - `idx_address_tenant_normalized` for dedup key lookups
  - `idx_address_tenant_not_deleted` for soft-delete filtering
- **RLS Policy**: tenant isolation via `app.tenant_id` setting

### Conventions Respected

✅ **typed_ids.md**: AddressId, TenantId wrappers  
✅ **command_query_handlers.md**: @UseCase annotation  
✅ **PLAYBOOK.md**: port abstraction, no direct repo access  
✅ **inter_domain_calls.md**: internal service, no catalog/address dependency  
✅ **RLS**: tenant-scoped via Postgres policies

---

## 🚀 Next Steps: PR2 — Migration

Replace all `catalog/address` dependencies:

1. Update `core/tenant`, `core/outlet`, `core/user` to use `AddressCrudService`
2. Remove `catalog/address` module entirely
3. Add ArchUnit guard: `!catalog.address`
4. Run integration tests

### Usage Example

```java
@RequiredArgsConstructor
class OutletService {
  private final AddressCrudService addressService;

  void configureOutlet(TenantId tenantId, OutletInput input) {
    // Upsert address (dedup automatic)
    UUID addressId = addressService.upsert(tenantId, input.addressInput());

    // Store reference only
    outlet.setAddressId(addressId);
  }
}
```

---

## ✅ Definition of Done Checklist

- [x] `core/address` module compiles and tests pass
- [x] Domain: Address record + normalizer + dedup key factory
- [x] Application: AddressCrudService, AddressInput, AddressView
- [x] Ports: AddressReaderPort, AddressWriterPort (abstraction)
- [x] Persistence: JPA entity, repository, adapter, mapper
- [x] Database: RLS enabled, unique constraint, indices
- [x] Deduplication: via unique index + exception handling (race-safe)
- [ ] Migrate `core/tenant`, `core/outlet`, `core/user` (PR2)
- [ ] Remove `catalog/address` entirely (PR3)
- [ ] Add ArchUnit guard (PR3)
