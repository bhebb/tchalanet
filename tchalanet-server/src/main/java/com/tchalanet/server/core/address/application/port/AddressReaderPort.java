package com.tchalanet.server.core.address.application.port;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.address.domain.Address;
import java.util.Optional;

/**
 * Port for reading addresses (tenant-scoped).
 * Per spec: minimal read operations with typed IDs throughout.
 * Per typed_ids.md: no raw UUID outside persistence layer.
 */
public interface AddressReaderPort {

  /**
   * Find address by ID within tenant scope.
   * RLS enforced at persistence layer.
   *
   * @param tenantId tenant scope (typed)
   * @param addressId address typed ID
   * @return address domain model if found and not deleted
   */
  Optional<Address> findById(TenantId tenantId, AddressId addressId);

  /**
   * MVP: Find the single active address for tenant.
   * Intended for simple cases where tenant has only one address.
   * Returns the single non-deleted address ID if exactly one exists, empty otherwise.
   *
   * @param tenantId tenant scope (typed)
   * @return active address ID if exists
   */
  Optional<AddressId> findActiveIdByTenant(TenantId tenantId);

  /**
   * Find address ID by normalized dedup key (tenant-scoped).
   * Used by dedup logic in AddressCrudService.
   * Useful for outlet/user to avoid duplicate address creation.
   *
   * @param tenantId tenant scope (typed)
   * @param normalizedKey SHA-256 hex dedup key
   * @return existing active address ID if found
   */
  Optional<AddressId> findIdByNormalizedKey(TenantId tenantId, String normalizedKey);
}
