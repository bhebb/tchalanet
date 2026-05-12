package com.tchalanet.server.platform.address.internal.service;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.address.api.model.AddressInput;
import com.tchalanet.server.platform.address.api.model.AddressView;
import com.tchalanet.server.platform.address.internal.adapter.AddressPersistenceAdapter;
import com.tchalanet.server.platform.address.internal.service.Address;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * CRUD service for addresses (tenant-scoped).
 * Provides two upsert strategies: generic (multi-address) and tenant-primary (mono-address MVP).
 * Per spec: service calculates keys, port receives complete Address objects.
 * Per typed_ids.md: all typed IDs throughout, no raw UUID in application layer.
 * Per PLAYBOOK.md: thin service, race-safe via unique constraints + exception handling.
 */
@UseCase
@RequiredArgsConstructor
public class AddressCrudService {

  private final Clock clock;

  private final AddressPersistenceAdapter persistenceAdapter;

  /**
   * Generic upsert with dedup by (tenant_id, normalized_key) for ACTIVE rows.
   *
   * - If a same normalized address already exists (active), returns existing id.
   * - Else inserts a new address.
   * - Race-safe: if unique constraint trips, re-lookup and return the winner id.
   *
   * Useful for outlet/user; OK for tenant too, but for tenant we prefer upsertTenantPrimary().
   */
  public AddressId upsert(TenantId tenantId, AddressInput input) {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(input, "input");

    final String key = dedupeKey(input);
    final Instant now = Instant.now(clock);

    var existing = persistenceAdapter.findIdByNormalizedKey(tenantId, key);
    if (existing.isPresent()) return existing.get();

    var address =
        Address.createNew(
            tenantId,
            input.line1(),
            input.line2(),
            input.city(),
            input.region(),
            input.country(),
            input.postalCode(),
            key,
            now);

    try {
      return persistenceAdapter.insert(address);
    } catch (DataIntegrityViolationException e) {
      // Another tx inserted the same key first
      return persistenceAdapter
          .findIdByNormalizedKey(tenantId, key)
          .orElseThrow(() -> e);
    }
  }


  /**
   * MVP: one active address per tenant.
   *
   * - If tenant has an active address: UPDATE it (same id).
   * - Else: INSERT new address.
   *
   * DB enforces uniqueness via: UNIQUE (tenant_id) WHERE deleted=false
   */
  public AddressId upsertTenantPrimary(TenantId tenantId, AddressInput input) {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(input, "input");

    final String key = dedupeKey(input);
    final Instant now = Instant.now(clock);

    var activeIdOpt = persistenceAdapter.findActiveIdByTenant(tenantId);
    if (activeIdOpt.isPresent()) {
      AddressId id = activeIdOpt.get();

      // We update the existing row to the new values (keep same ID).
      // Note: we do NOT create a second active row (DB prevents it anyway).
      Address updated =
          Address.restore(
              id,
              tenantId,
              input.line1(),
              input.line2(),
              input.city(),
              input.region(),
              input.country(),
              input.postalCode(),
              key,
              /*deleted*/ false,
              /*createdAt*/ null, // impl should keep createdAt; see notes below
              /*updatedAt*/ now);

      persistenceAdapter.update(updated);
      return id;
    }

    // No active address yet -> insert
    Address created =
        Address.createNew(
            tenantId,
            input.line1(),
            input.line2(),
            input.city(),
            input.region(),
            input.country(),
            input.postalCode(),
            key,
            now);

    try {
      return persistenceAdapter.insert(created);
    } catch (DataIntegrityViolationException e) {
      // Race: someone inserted the active address first. Return the now-active id.
      return persistenceAdapter
          .findActiveIdByTenant(tenantId)
          .orElseThrow(() -> e);
    }
  }

  /**
   * Get address view by ID.
   * Returns immutable projection for API responses.
   *
   * @param tenantId tenant scope (typed)
   * @param id address ID (typed)
   * @return address view if found and not deleted
   */
  public Optional<AddressView> get(TenantId tenantId, AddressId id) {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(id, "id");

    return persistenceAdapter.findById(tenantId, id).map(AddressView::fromDomain);
  }

  /**
   * MVP soft-delete.
   * With your partial unique indexes, this allows recreating the same address later.
   */
  public void softDelete(TenantId tenantId, AddressId id) {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(id, "id");

    // Read first to enforce tenant-scope at app level too (RLS is still the last line).
    var existing = persistenceAdapter.findById(tenantId, id);
    if (existing.isEmpty()) return; // idempotent

    persistenceAdapter.softDelete(id);
  }

  /**
   * Optional MVP helper:
   * update-by-id (used by tenant/outlet/user when they keep the same address_id).
   *
   * If you don't want "update collision merge" now, you can let the unique index throw.
   */
  public void update(TenantId tenantId, AddressId id, AddressInput input) {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(input, "input");

    throw new UnsupportedOperationException("Address update path not yet migrated to the platform API");
  }

  private static String dedupeKey(AddressInput input) {
    return Integer.toHexString(
        Objects.hash(
            safe(input.line1()),
            safe(input.line2()),
            safe(input.city()),
            safe(input.region()),
            safe(input.country()),
            safe(input.postalCode())));
  }

  private static String safe(String value) {
    return value == null ? "" : value.trim().toLowerCase();
  }
}
