package com.tchalanet.server.catalog.i18n.internal.write;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesAdminCatalog;
import com.tchalanet.server.catalog.i18n.api.model.CreateI18nOverrideAdminRequest;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel;
import com.tchalanet.server.catalog.i18n.internal.web.model.CreateI18nOverrideRequest;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideView;
import com.tchalanet.server.catalog.i18n.internal.web.model.UpdateI18nOverrideRequest;
import com.tchalanet.server.catalog.i18n.internal.cache.I18nOverridesCacheNames;
import com.tchalanet.server.catalog.i18n.internal.mapper.I18nOverrideMapper;
import com.tchalanet.server.catalog.i18n.internal.persistence.I18nOverrideEntity;
import com.tchalanet.server.catalog.i18n.internal.persistence.I18nOverrideRepository;
import com.tchalanet.server.common.types.id.I18nOverrideId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * I18n Overrides Admin Service (WRITE SIDE)
 *
 * <p>Handles all write operations for i18n overrides (create, update, delete). This service is
 * ONLY used by admin controllers and MUST NOT be called by core/features.
 *
 * <p>All writes trigger cache eviction via {@code @CacheEvict}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class I18nOverridesAdminService implements I18nOverridesAdminCatalog {

  private final I18nOverrideRepository repository;
  private final I18nOverrideMapper mapper;

  /**
   * Create a new i18n override.
   *
   * @param request create request
   * @return created override view
   * @throws IllegalArgumentException if validation fails or duplicate exists
   */
  @Transactional
  @CacheEvict(
      cacheNames = {
        I18nOverridesCacheNames.BY_TENANT,
        I18nOverridesCacheNames.BY_TENANT_LOCALE,
        I18nOverridesCacheNames.BY_ID
      },
      allEntries = true)
  public I18nOverrideView create(CreateI18nOverrideRequest request) {
    log.info(
        "Creating i18n override: tenant={}, locale={}, key={}, level={}",
        request.tenantId(),
        request.locale(),
        request.i18nKey(),
        request.level());

    // Validate required fields
    validateRequest(request);

    // Check for duplicates
    checkUniqueness(request);

    // Create entity
      var entity = createI18nOverrideEntity(request);

      entity = repository.save(entity);
    log.info("Created i18n override with ID: {}", entity.getId());

    return mapper.toView(entity);
  }

  @Override
  @Transactional
  @CacheEvict(
      cacheNames = {
        I18nOverridesCacheNames.BY_TENANT,
        I18nOverridesCacheNames.BY_TENANT_LOCALE,
        I18nOverridesCacheNames.BY_ID
      },
      allEntries = true)
  public I18nOverrideView create(CreateI18nOverrideAdminRequest request) {
    return create(
        new CreateI18nOverrideRequest(
            request.tenantId(),
            request.locale(),
            request.level(),
            request.i18nKey(),
            request.i18nValue()));
  }

    private static @NonNull I18nOverrideEntity createI18nOverrideEntity(CreateI18nOverrideRequest request) {
        var entity = new I18nOverrideEntity();
        entity.setId(UUID.randomUUID());
        entity.setLevel(request.level());
        // tenantId must be null for GLOBAL level
        if (request.level() == com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel.GLOBAL) {
          entity.setTenantId(null);
        } else {
          entity.setTenantId(request.tenantId().value());
        }
        entity.setLocale(request.locale());
        entity.setI18nKey(request.i18nKey());
        entity.setI18nValue(request.i18nValue());
        entity.setActive(true);
        return entity;
    }

    /**
   * Update an existing i18n override.
   *
   * @param id override ID
   * @param request update request
   * @return updated override view
   * @throws IllegalArgumentException if not found
   */
  @Transactional
  @CacheEvict(
      cacheNames = {
        I18nOverridesCacheNames.BY_TENANT,
        I18nOverridesCacheNames.BY_TENANT_LOCALE,
        I18nOverridesCacheNames.BY_ID
      },
      allEntries = true)
  public I18nOverrideView update(I18nOverrideId id, UpdateI18nOverrideRequest request) {
    log.info("Updating i18n override ID: {}", id);

    I18nOverrideEntity entity =
        repository
            .findByIdAndDeletedAtIsNull(id.value())
            .orElseThrow(() -> new IllegalArgumentException("I18n override not found: " + id));

    // Update level (if provided)
    if (request.level() != null) {
      entity.setLevel(request.level());
      // adjust tenantId according to level
      if (request.level() == I18nOverrideLevel.GLOBAL) {
        entity.setTenantId(null);
      }
    }

    // Update value (if provided)
    if (request.i18nValue() != null && !request.i18nValue().isBlank()) {
      entity.setI18nValue(request.i18nValue());
    }

    // Update active status (if provided)
    if (request.active() != null) {
      entity.setActive(request.active());
    }

    entity = repository.save(entity);
    log.info("Updated i18n override: {}", entity.fullKey());

    return mapper.toView(entity);
  }

  /**
   * Delete an i18n override (soft delete).
   *
   * @param id override ID
   * @throws IllegalArgumentException if not found
   */
  @Transactional
  @CacheEvict(
      cacheNames = {
        I18nOverridesCacheNames.BY_TENANT,
        I18nOverridesCacheNames.BY_TENANT_LOCALE,
        I18nOverridesCacheNames.BY_ID
      },
      allEntries = true)
  public void delete(I18nOverrideId id) {
    log.info("Deleting i18n override ID: {}", id);

    var entity =
        repository
            .findByIdAndDeletedAtIsNull(id.value())
            .orElseThrow(() -> new IllegalArgumentException("I18n override not found: " + id));

    // Soft delete
    entity.setDeletedAt(Instant.now());
    entity.setActive(false);
    repository.save(entity);

    log.info("Soft-deleted i18n override: {}", entity.fullKey());
  }

  // ========================================
  // Validation helpers
  // ========================================

  private void validateRequest(CreateI18nOverrideRequest request) {
    if (request.level() == null) {
      throw new IllegalArgumentException("level is required");
    }

    if (request.level() == com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel.TENANT
        && request.tenantId() == null) {
      throw new IllegalArgumentException("tenantId is required for TENANT level");
    }

    if (request.locale() == null || request.locale().isBlank()) {
      throw new IllegalArgumentException("locale is required");
    }
    if (request.i18nKey() == null || request.i18nKey().isBlank()) {
      throw new IllegalArgumentException("i18nKey is required");
    }
    if (request.i18nValue() == null || request.i18nValue().isBlank()) {
      throw new IllegalArgumentException("i18nValue is required");
    }

    // Validate locale format (simple check: 2-5 chars, lowercase)
    if (!request.locale().matches("[a-z]{2,5}")) {
      throw new IllegalArgumentException(
          "Invalid locale format: " + request.locale() + " (expected: fr, en, ht, etc.)");
    }
  }

  private void checkUniqueness(CreateI18nOverrideRequest request) {
    // For tenant level we check tenant/locale/key uniqueness; for global we check locale/key/level
    Optional<I18nOverrideEntity> existing;
    if (request.level() == com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel.TENANT) {
      existing = repository.findFirstByTenantIdAndLocaleAndI18nKeyAndActiveTrue(
          request.tenantId().value(), request.locale(), request.i18nKey());
    } else {
      existing = repository.findFirstByLocaleAndI18nKeyAndLevel(request.locale(), request.i18nKey(), request.level());
    }

    if (existing.isPresent()) {
      throw new IllegalArgumentException(
          "I18n override already exists: "
              + request.locale()
              + ":"
              + request.i18nKey()
              + " for tenant "
              + request.tenantId());
    }
  }


}
