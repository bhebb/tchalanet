package com.tchalanet.server.catalog.tenant.internal.mapper;

import com.tchalanet.server.catalog.tenant.api.model.TenantBootstrapView;
import com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView;
import com.tchalanet.server.catalog.tenant.internal.persistence.TenantRegistryJpaEntity;
import com.tchalanet.server.common.mapper.CommonIdMapper;

import java.time.ZoneId;
import java.util.Currency;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for tenant registry views.
 * Per DOMAIN_TENANT_CATALOG.md: maps entity → view with safe conversions.
 * Handles ZoneId and Currency parsing with sensible fallbacks.
 */
@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface TenantRegistryMapper {

  /**
   * Map JPA entity to bootstrap view.
   * Parses timezone and currency with fallbacks.
   */
  @Mapping(target = "tenantId", source = "id")
  @Mapping(target = "timezone", expression = "java(safeZoneId(entity.getTimezone()))")
  @Mapping(target = "currency", expression = "java(safeCurrency(entity.getCurrency()))")
  @Mapping(target = "defaultLanguage", expression = "java((entity.getDefaultLanguage() == null || entity.getDefaultLanguage().isBlank()) ? \"fr\" : entity.getDefaultLanguage())")
  @Mapping(target = "defaultLocale", expression = "java((entity.getDefaultLocale() == null || entity.getDefaultLocale().isBlank()) ? \"fr-HT\" : entity.getDefaultLocale())")
  TenantBootstrapView toBootstrapView(TenantRegistryJpaEntity entity);

  /**
   * Map JPA entity to registry view.
   * Includes timezone, currency, and typed ThemePresetId.
   */
  @Mapping(target = "tenantId", source = "id")
  @Mapping(target = "timezone", expression = "java(safeZoneId(entity.getTimezone()))")
  @Mapping(target = "currency", expression = "java(safeCurrency(entity.getCurrency()))")
  @Mapping(target = "defaultLanguage", expression = "java((entity.getDefaultLanguage() == null || entity.getDefaultLanguage().isBlank()) ? \"fr\" : entity.getDefaultLanguage())")
  @Mapping(target = "defaultLocale", expression = "java((entity.getDefaultLocale() == null || entity.getDefaultLocale().isBlank()) ? \"fr-HT\" : entity.getDefaultLocale())")
  @Mapping(target = "addressId", expression = "java(entity.getAddressId() != null ? Optional.of(com.tchalanet.server.common.types.id.AddressId.of(entity.getAddressId())) : Optional.empty())")
  @Mapping(target = "activeThemeId", expression = "java(entity.getActiveThemeId() != null ? Optional.of(com.tchalanet.server.common.types.id.ThemePresetId.of(entity.getActiveThemeId())) : Optional.empty())")
  TenantRegistryView toRegistryView(TenantRegistryJpaEntity entity);

  /**
   * Safe ZoneId parsing with UTC fallback.
   */
  default ZoneId safeZoneId(String raw) {
    try {
      return (raw == null || raw.isBlank()) ? ZoneId.of("UTC") : ZoneId.of(raw);
    } catch (Exception e) {
      return ZoneId.of("UTC");
    }
  }

  /**
   * Safe Currency parsing with USD fallback.
   */
  default Currency safeCurrency(String raw) {
    try {
      return (raw == null || raw.isBlank()) ? Currency.getInstance("USD") : Currency.getInstance(raw);
    } catch (Exception e) {
      return Currency.getInstance("USD");
    }
  }

}
