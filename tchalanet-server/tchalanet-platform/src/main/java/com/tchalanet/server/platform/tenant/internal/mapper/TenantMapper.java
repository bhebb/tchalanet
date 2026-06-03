package com.tchalanet.server.platform.tenant.internal.mapper;

import com.tchalanet.server.common.json.utils.JsonUtilsHolder;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.platform.tenant.internal.domain.TenantConfig;
import com.tchalanet.server.platform.tenant.internal.persistence.TenantJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import tools.jackson.databind.JsonNode;

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
  @Mapping(target = "config", expression = "java(readConfig(entity.getConfig()))")
  TenantConfig toDomain(TenantJpaEntity entity);

  @Mapping(target = "id", source = "tenant.id")
  @Mapping(target = "addressId", source = "tenant.addressId")
  @Mapping(target = "activeThemeId", source = "tenant.activeThemeId")
  @Mapping(target = "timezone", source = "tenant.timezone")
  @Mapping(target = "currency", expression = "java(tenant.currency() != null ? tenant.currency().getCurrencyCode() : null)")
  @Mapping(target = "config", expression = "java(writeConfig(tenant.config()))")
  @Mapping(target = "defaultLanguage", expression = "java(extractLocaleField(tenant.config(), \"defaultLanguage\", \"fr\"))")
  @Mapping(target = "defaultLocale", expression = "java(extractLocaleField(tenant.config(), \"defaultLocale\", \"fr-HT\"))")
  @Mapping(target = "version", constant = "0L")
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
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
  @Mapping(target = "config", expression = "java(writeConfig(tenant.config()))")
  @Mapping(target = "defaultLanguage", expression = "java(extractLocaleField(tenant.config(), \"defaultLanguage\", entity.getDefaultLanguage()))")
  @Mapping(target = "defaultLocale", expression = "java(extractLocaleField(tenant.config(), \"defaultLocale\", entity.getDefaultLocale()))")
  @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
  @Mapping(target = "updatedBy", ignore = true)
  void updateEntity(TenantConfig tenant, @MappingTarget TenantJpaEntity entity);

  default JsonNode readConfig(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return JsonUtilsHolder.get().parse(raw);
  }

  default String writeConfig(JsonNode node) {
    if (node == null) {
      return "{}";
    }
    return JsonUtilsHolder.get().toJson(node);
  }

  default String extractLocaleField(JsonNode config, String field, String defaultValue) {
    if (config == null) return defaultValue;
    JsonNode locale = config.get("locale");
    if (locale == null || !locale.has(field)) return defaultValue;
    String value = locale.get(field).asText(null);
    return (value == null || value.isBlank()) ? defaultValue : value;
  }
}

