package com.tchalanet.server.platform.address.internal.mapper;

import com.tchalanet.server.platform.address.internal.persistence.AddressJpaEntity;
import com.tchalanet.server.platform.address.internal.service.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

  public Address toDomain(AddressJpaEntity entity) {
    return new Address(
        com.tchalanet.server.common.types.id.AddressId.of(entity.getId()),
        com.tchalanet.server.common.types.id.TenantId.nullableOf(entity.getTenantId()),
        entity.getLine1(),
        entity.getLine2(),
        entity.getCity(),
        entity.getRegion(),
        entity.getCountry(),
        entity.getPostalCode(),
        entity.getNormalizedKey(),
        entity.isDeleted(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public AddressJpaEntity toEntity(Address domain) {
    var entity = new AddressJpaEntity();
    updateEntityFromDomain(domain, entity);
    return entity;
  }

  public void updateEntityFromDomain(Address domain, AddressJpaEntity entity) {
    entity.setLine1(domain.line1());
    entity.setLine2(domain.line2());
    entity.setCity(domain.city());
    entity.setRegion(domain.region());
    entity.setCountry(domain.country());
    entity.setPostalCode(domain.postalCode());
    entity.setNormalizedKey(domain.normalizedKey());
    entity.setDeleted(domain.deleted());
    entity.setCreatedAt(domain.createdAt());
    entity.setUpdatedAt(domain.updatedAt());
    if (domain.id() != null) {
      entity.setId(domain.id().value());
    }
    if (domain.tenantId() != null) {
      entity.setTenantId(domain.tenantId().value());
    }
  }
}

