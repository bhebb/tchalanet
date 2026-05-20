package com.tchalanet.server.platform.address.internal.adapter;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.address.internal.mapper.AddressMapper;
import com.tchalanet.server.platform.address.internal.persistence.AddressJpaEntity;
import com.tchalanet.server.platform.address.internal.persistence.AddressJpaRepository;
import com.tchalanet.server.platform.address.internal.service.Address;
import com.tchalanet.server.platform.address.internal.service.AddressReaderPort;
import com.tchalanet.server.platform.address.internal.service.AddressWriterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Adapter implementing Address persistence ports.
 * Per spec: handles dedup via partial unique index on (tenant_id, normalized_key) WHERE deleted=false.
 * Per typed_ids.md: uses typed IDs (TenantId, AddressId) throughout.
 * Per request_context_usage.md: RLS enforced at DB level via Postgres policies.
 */
@Component
@RequiredArgsConstructor
public class AddressPersistenceAdapter implements AddressReaderPort, AddressWriterPort {

  private final AddressJpaRepository repository;
  private final AddressMapper mapper;

  @Override
  public Optional<Address> findById(TenantId tenantId, AddressId addressId) {
    return repository.findByIdAndTenantIdAndDeletedFalse(addressId.value(), tenantId.value())
        .map(mapper::toDomain);
  }

  @Override
  public Optional<AddressId> findActiveIdByTenant(TenantId tenantId) {
    // MVP: find the single active address for tenant (if exactly one)
    var results = repository.findByTenantIdAndDeletedFalse(tenantId.value());
    if (results.size() == 1) {
      return Optional.of(AddressId.of(results.get(0).getId()));
    }
    return Optional.empty();
  }

  @Override
  public Optional<AddressId> findIdByNormalizedKey(TenantId tenantId, String normalizedKey) {
    return repository.findByTenantIdAndNormalizedKeyAndDeletedFalse(tenantId.value(), normalizedKey)
        .map(entity -> AddressId.of(entity.getId()));
  }

  @Override
  @Transactional
  public AddressId insert(Address address) {
    var entity = mapper.toEntity(address);
    var saved = repository.save(entity);
    return AddressId.of(saved.getId());
  }

  @Override
  @Transactional
  public void update(Address address) {
    var existing = repository.findByIdAndTenantIdAndDeletedFalse(address.id().value(), address.tenantId().value())
        .orElseThrow(() -> new IllegalArgumentException("Address not found: " + address.id()));

    mapper.updateEntityFromDomain(address, existing);
    repository.save(existing);
  }

  @Override
  @Transactional
  public void softDelete(AddressId addressId) {
    var existing = repository.findById(addressId.value())
        .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));

    existing.setDeleted(true);
    existing.setDeletedAt(Instant.now());
    repository.save(existing);
  }
}
