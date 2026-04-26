package com.tchalanet.server.core.tenantconfig.infra.persistence.mapper;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.core.tenantconfig.domain.model.TenantConfig;
import com.tchalanet.server.core.tenantconfig.infra.persistence.TenantJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper: TenantConfig <→> TenantJpaEntity.
 */
@Mapper(componentModel = "spring", uses = {CommonIdMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {

  @Mapping(target = "id", source = "entity.id")
  @Mapping(target = "addressId", source = "entity.addressId")
  @Mapping(target = "activeThemeId", source = "entity.activeThemeId")
  @Mapping(target = "timezone", source = "entity.timezone")
  @Mapping(target = "currency", expression = "java(entity.getCurrency() != null ? java.util.Currency.getInstance(entity.getCurrency()) : null)")
  TenantConfig toDomain(TenantJpaEntity entity);

  @Mapping(target = "id", source = "tenant.id")
  @Mapping(target = "addressId", source = "tenant.addressId")
  @Mapping(target = "activeThemeId", source = "tenant.activeThemeId")
  @Mapping(target = "timezone", source = "tenant.timezone")
  @Mapping(target = "currency", expression = "java(tenant.currency() != null ? tenant.currency().getCurrencyCode() : null)")
  @Mapping(target = "version", constant = "0L")
  @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
  @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  TenantJpaEntity toEntity(TenantConfig tenant);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "code", source = "tenant.code")
  @Mapping(target = "name", source = "tenant.name")
  @Mapping(target = "type", source = "tenant.type")
  @Mapping(target = "timezone", source = "tenant.timezone")
  @Mapping(target = "currency", expression = "java(tenant.currency() != null ? tenant.currency().getCurrencyCode() : null)")
  @Mapping(target = "status", source = "tenant.status")
  @Mapping(target = "addressId", source = "tenant.addressId")
  @Mapping(target = "activeThemeId", source = "tenant.activeThemeId")
  @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
  @Mapping(target = "updatedBy", ignore = true)
  void updateEntity(TenantConfig tenant, @MappingTarget TenantJpaEntity entity);
}
