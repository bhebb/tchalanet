package com.tchalanet.server.core.address.infra.persistence.mapper;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.core.address.domain.Address;
import com.tchalanet.server.core.address.infra.persistence.AddressJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for Address (JPA entity <-> domain model).
 * Uses CommonIdMapper for typed ID conversions.
 */
@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface AddressMapper {

  Address toDomain(AddressJpaEntity entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  AddressJpaEntity toEntity(Address domain);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "tenantId", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  void updateEntityFromDomain(Address domain, @MappingTarget AddressJpaEntity entity);
}
