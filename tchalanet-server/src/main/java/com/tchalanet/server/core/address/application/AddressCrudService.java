package com.tchalanet.server.core.address.application;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.address.application.model.AddressInput;
import com.tchalanet.server.core.address.application.model.AddressView;
import com.tchalanet.server.core.address.application.port.AddressReaderPort;
import com.tchalanet.server.core.address.application.port.AddressWriterPort;
import com.tchalanet.server.core.address.domain.Address;
import com.tchalanet.server.core.address.domain.AddressDedupeKeyFactory;
import com.tchalanet.server.core.address.domain.AddressNormalizer;
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

  private final AddressReaderPort reader;
  private final AddressWriterPort writer;
  private final Clock clock;

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

    final String normalized = AddressNormalizer.normalize(
        input.line1(),
        input.line2(),
        input.city(),
        input.region(),
        input.country(),
        input.postalCode()
    );
    final String key = AddressDedupeKeyFactory.sha256Hex(normalized);
    final Instant now = Instant.now(clock);

    var existing = reader.findIdByNormalizedKey(tenantId, key);
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
      return writer.insert(address);
    } catch (DataIntegrityViolationException e) {
      // Another tx inserted the same key first
      return reader
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

    final String normalized = AddressNormalizer.normalize(
        input.line1(),
        input.line2(),
        input.city(),
        input.region(),
        input.country(),
        input.postalCode()
    );
    final String key = AddressDedupeKeyFactory.sha256Hex(normalized);
    final Instant now = Instant.now(clock);

    var activeIdOpt = reader.findActiveIdByTenant(tenantId);
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

      writer.update(updated);
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
      return writer.insert(created);
    } catch (DataIntegrityViolationException e) {
      // Race: someone inserted the active address first. Return the now-active id.
      return reader
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

    return reader.findById(tenantId, id).map(AddressView::fromDomain);
  }

  /**
   * MVP soft-delete.
   * With your partial unique indexes, this allows recreating the same address later.
   */
  public void softDelete(TenantId tenantId, AddressId id) {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(id, "id");

    // Read first to enforce tenant-scope at app level too (RLS is still the last line).
    var existing = reader.findById(tenantId, id);
    if (existing.isEmpty()) return; // idempotent

    writer.softDelete(id);
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

    // Ensure it exists & belongs to tenant
    Address existing =
        reader.findById(tenantId, id)
            .orElseThrow(() -> new IllegalArgumentException("Address not found"));

    final String normalized = AddressNormalizer.normalize(
        input.line1(),
        input.line2(),
        input.city(),
        input.region(),
        input.country(),
        input.postalCode()
    );
    final String key = AddressDedupeKeyFactory.sha256Hex(normalized);
    final Instant now = Instant.now(clock);

    Address updated =
        Address.restore(
            existing.id(),
            existing.tenantId(),
            input.line1(),
            input.line2(),
            input.city(),
            input.region(),
            input.country(),
            input.postalCode(),
            key,
            existing.deleted(),
            existing.createdAt(),
            now);

    writer.update(updated);
  }
}
