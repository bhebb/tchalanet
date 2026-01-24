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
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * CRUD service for addresses (tenant-scoped).
 * Provides upsert with deduplication and CRUD operations.
 * Per spec: uses ports for insert/update/softDelete operations.
 * Per typed_ids.md: all typed IDs throughout, no raw UUID in application layer.
 */
@UseCase
@RequiredArgsConstructor
public class AddressCrudService {

  private final AddressReaderPort readerPort;
  private final AddressWriterPort writerPort;

  /**
   * Upsert address with deduplication.
   * If address already exists (by normalized key) → return existing ID.
   * Otherwise → create + return new ID.
   *
   * Per spec: service calculates normalizedKey, port receives complete Address.
   *
   * @param tenantId tenant scope (typed)
   * @param input address fields
   * @return address ID (existing or newly created, typed)
   */
  public AddressId upsert(TenantId tenantId, AddressInput input) {
    // Service responsibility: calculate normalized key
    String normalizedKey = AddressDedupeKeyFactory.generateKeyFromFields(
        input.line1(),
        input.line2(),
        input.city(),
        input.region(),
        input.country(),
        input.postalCode()
    );

    // Check if already exists by dedup key
    var existingId = readerPort.findIdByNormalizedKey(tenantId, normalizedKey);
    if (existingId.isPresent()) {
      return existingId.get();
    }

    // Create new address (with key already calculated)
    var newAddress = new Address(
        AddressId.of(UUID.randomUUID()),
        tenantId,
        input.line1(),
        input.line2(),
        input.city(),
        input.region(),
        input.country(),
        input.postalCode(),
        normalizedKey,
        false,
        Instant.now(),
        Instant.now()
    );

    // Port receives complete Address object only
    return writerPort.insert(newAddress);
  }

  /**
   * Get address view by ID.
   * Returns immutable projection for API responses.
   *
   * @param tenantId tenant scope (typed)
   * @param addressId address ID (typed)
   * @return address view if found and not deleted
   */
  public Optional<AddressView> get(TenantId tenantId, AddressId addressId) {
    return readerPort.findById(tenantId, addressId)
        .map(this::toView);
  }

  /**
   * Update address fields.
   * Per spec: implementation ensures tenant-scope safety via RLS.
   *
   * @param tenantId tenant scope (typed)
   * @param addressId address ID (typed)
   * @param input new address fields
   * @throws IllegalArgumentException if address not found
   */
  public void update(TenantId tenantId, AddressId addressId, AddressInput input) {
    var existing = readerPort.findById(tenantId, addressId)
        .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));

    // Recalculate normalized key from updated input
    String newNormalizedKey = AddressDedupeKeyFactory.generateKeyFromFields(
        input.line1(),
        input.line2(),
        input.city(),
        input.region(),
        input.country(),
        input.postalCode()
    );

    var updatedAddress = new Address(
        existing.id(),
        existing.tenantId(),
        input.line1(),
        input.line2(),
        input.city(),
        input.region(),
        input.country(),
        input.postalCode(),
        newNormalizedKey,
        false, // keep active
        existing.createdAt(),
        Instant.now()
    );

    writerPort.update(updatedAddress);
  }

  /**
   * Soft delete address.
   * Per spec: implementation ensures tenant-scope safety via RLS.
   *
   * @param addressId address ID to delete (typed)
   * @throws IllegalArgumentException if address not found
   */
  public void softDelete(AddressId addressId) {
    writerPort.softDelete(addressId);
  }

  /**
   * Convert domain Address to view DTO for API responses.
   */
  private AddressView toView(Address address) {
    return new AddressView(
        address.id(),
        address.line1(),
        address.line2(),
        address.city(),
        address.region(),
        address.country(),
        address.postalCode()
    );
  }
}
