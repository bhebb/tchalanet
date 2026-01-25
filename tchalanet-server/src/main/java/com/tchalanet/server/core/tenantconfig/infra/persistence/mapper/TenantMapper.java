package com.tchalanet.server.core.tenantconfig.infra.persistence.mapper;

import com.tchalanet.server.core.tenantconfig.domain.model.TenantConfig;
import com.tchalanet.server.core.tenantconfig.infra.persistence.TenantJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper: TenantConfig <→> TenantJpaEntity.
 * Per typed_ids.md: UUID in entity, typed IDs in domain
 */
@Mapper(componentModel = "spring")
public interface TenantMapper {

  /**
   * Map JPA entity to domain model.
   * Converts UUID to typed IDs (TenantId, AddressId, ThemePresetId).
   */
  @Mapping(target = "id", expression = "java(com.tchalanet.server.common.types.id.TenantId.of(entity.getId()))")
  @Mapping(target = "addressId", expression = "java(entity.getAddressId() != null ? com.tchalanet.server.common.types.id.AddressId.of(entity.getAddressId()) : null)")
  @Mapping(target = "activeThemeId", expression = "java(entity.getActiveThemeId() != null ? com.tchalanet.server.common.types.id.ThemePresetId.of(entity.getActiveThemeId()) : null)")
  TenantConfig toDomain(TenantJpaEntity entity);

  /**
   * Map domain model to JPA entity (for creation).
   * Converts typed IDs back to UUID.
   */
  @Mapping(target = "id", expression = "java(tenant.id().value())")
  @Mapping(target = "addressId", expression = "java(tenant.addressId() != null ? tenant.addressId().value() : null)")
  @Mapping(target = "activeThemeId", expression = "java(tenant.activeThemeId() != null ? tenant.activeThemeId().value() : null)")
  @Mapping(target = "version", expression = "java(0L)")
  @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
  @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  TenantJpaEntity toEntity(TenantConfig tenant);

  /**
   * Update existing JPA entity from domain model (for updates).
   * Preserves id, version, createdAt, createdBy (managed by JPA/auditing).
   * Only updates mutable fields.
   */
  @Mapping(target = "id", ignore = true)  // never change ID
  @Mapping(target = "version", ignore = true)  // managed by JPA optimistic locking
  @Mapping(target = "createdAt", ignore = true)  // never change creation timestamp
  @Mapping(target = "createdBy", ignore = true)  // never change creator
  @Mapping(target = "deletedAt", ignore = true)  // managed separately (soft delete)
  @Mapping(target = "code", source = "tenant.code")
  @Mapping(target = "name", source = "tenant.name")
  @Mapping(target = "type", source = "tenant.type")
  @Mapping(target = "timezone", expression = "java(tenant.timezone().getId())")
  @Mapping(target = "currency", expression = "java(tenant.currency().getCurrencyCode())")
  @Mapping(target = "status", source = "tenant.status")
  @Mapping(target = "addressId", expression = "java(tenant.addressId() != null ? tenant.addressId().value() : null)")
  @Mapping(target = "activeThemeId", expression = "java(tenant.activeThemeId() != null ? tenant.activeThemeId().value() : null)")
  @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
  @Mapping(target = "updatedBy", ignore = true)  // managed by JPA auditing
  void updateEntity(TenantConfig tenant, @MappingTarget TenantJpaEntity entity);
}
