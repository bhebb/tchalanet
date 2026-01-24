package com.tchalanet.server.core.address.application.port;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.core.address.domain.Address;

/**
 * Port for writing addresses (tenant-scoped).
 * Per spec: simple operations (insert, update, softDelete).
 * Per typed_ids.md: returns AddressId typed wrapper, not raw UUID.
 *
 * NOTE: normalizedKey is calculated by the service layer,
 * NOT by callers. The port receives complete Address objects only.
 */
public interface AddressWriterPort {

  /**
   * Insert a new address row.
   * Address must contain:
   * - tenantId (tenant scope)
   * - normalizedKey (calculated by service)
   * - deleted=false (new addresses are always active)
   *
   * @param address complete address with all required fields
   * @return generated address ID (typed)
   */
  AddressId insert(Address address);

  /**
   * Update an existing address row.
   * Implementation must ensure tenant-scope safety (RLS + guarded lookup).
   * Typically used for changing address fields (line1, city, etc).
   *
   * @param address address with updated fields (must contain id and tenantId)
   * @throws IllegalArgumentException if address not found for tenant
   */
  void update(Address address);

  /**
   * Soft-delete an address row (set deleted=true, deleted_at=now()).
   * Implementation must ensure tenant-scope safety (RLS + guarded lookup).
   *
   * @param addressId address ID to delete (typed)
   * @throws IllegalArgumentException if address not found
   */
  void softDelete(AddressId addressId);
}
