package com.tchalanet.server.catalog.settings.internal.write;

import com.tchalanet.server.catalog.settings.api.SettingsAdminCatalog;
import com.tchalanet.server.catalog.settings.api.model.CreateSettingAdminRequest;
import com.tchalanet.server.catalog.settings.api.model.SearchSettingsAdminCriteria;
import com.tchalanet.server.catalog.settings.api.model.SettingView;
import com.tchalanet.server.catalog.settings.internal.cache.SettingsCacheNames;
import com.tchalanet.server.catalog.settings.internal.mapper.SettingMapper;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingEntity;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingRepository;
import com.tchalanet.server.catalog.settings.internal.registry.SettingsValidator;
import com.tchalanet.server.catalog.settings.internal.web.model.CreateSettingRequest;
import com.tchalanet.server.catalog.settings.internal.web.model.SearchSettingsCriteria;
import com.tchalanet.server.catalog.settings.internal.web.model.UpdateSettingRequest;
import com.tchalanet.server.common.types.id.SettingId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settings Admin Service (WRITE SIDE)
 *
 * <p>Handles all write operations for settings (create, update, delete). This service is ONLY used
 * by admin controllers and MUST NOT be called by core/features.
 *
 * <p>All writes trigger cache eviction via {@code @CacheEvict}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsAdminService implements SettingsAdminCatalog {

  private final SettingRepository repository;
  private final SettingMapper mapper;

  /**
   * Search settings with pagination and filtering.
   *
   * @param criteria search criteria
   * @param pageRequest pagination parameters
   * @return page of settings
   */
  @Transactional(readOnly = true)
  public TchPage<SettingView> search(SearchSettingsCriteria criteria, TchPageRequest pageRequest) {
    Specification<SettingEntity> spec = buildSpecification(criteria);

    PageRequest springPageRequest =
        PageRequest.of(
            pageRequest.pageable().getPageNumber(),
            pageRequest.pageable().getPageSize(),
            Sort.by(Sort.Order.asc("namespace"), Sort.Order.asc("settingKey")));

    Page<SettingEntity> page = repository.findAll(spec, springPageRequest);

    return TchPage.of(
        mapper.toViews(page.getContent()),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast(),
        page.hasNext(),
        page.hasPrevious());
  }

  @Override
  @Transactional(readOnly = true)
  public TchPage<SettingView> search(SearchSettingsAdminCriteria criteria, TchPageRequest pageRequest) {
    return search(
        new SearchSettingsCriteria(
            criteria.namespace(),
            criteria.settingKey(),
            criteria.level(),
            criteria.tenantId(),
            criteria.active()),
        pageRequest);
  }

  /**
   * Get setting by ID.
   *
   * @param id setting ID
   * @return setting view
   * @throws IllegalArgumentException if not found
   */
  @Transactional(readOnly = true)
  public SettingView getById(SettingId id) {
    SettingEntity entity =
        repository
            .findById(id.value())
            .orElseThrow(() -> new IllegalArgumentException("Setting not found: " + id));
    return mapper.toView(entity);
  }

  /**
   * Create a new setting.
   *
   * @param request create request
   * @return created setting view
   * @throws IllegalArgumentException if validation fails or duplicate exists
   */
  @Transactional
  @CacheEvict(cacheNames = SettingsCacheNames.RESOLVED_SETTINGS, allEntries = true)
  public SettingView create(CreateSettingRequest request) {
    log.info("Creating setting: {}.{}", request.namespace(), request.settingKey());

    // 1. Validate setting key and value
    SettingsValidator.validateOrThrow(
        request.namespace(), request.settingKey(), request.valueType(), request.settingValue());

    // 2. Check for duplicates (active, non-deleted settings with same key at same level/limitScopeRef)
    checkUniqueness(request);

    // 3. Validate level-specific requirements
    validateLevelRequirements(request);

    // 4. Create entity
      var entity = createSettingEntity(request);

      entity = repository.save(entity);
    log.info("Created setting: {} with ID: {}", entity.fullKey(), entity.getId());

    return mapper.toView(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = SettingsCacheNames.RESOLVED_SETTINGS, allEntries = true)
  public SettingView create(CreateSettingAdminRequest request) {
    return create(
        new CreateSettingRequest(
            request.namespace(),
            request.settingKey(),
            request.settingValue(),
            request.valueType(),
            request.level(),
            request.tenantId(),
            request.outletId(),
            request.terminalId()));
  }

    private static @NonNull SettingEntity createSettingEntity(CreateSettingRequest request) {
        SettingEntity entity = new SettingEntity();
        entity.setNamespace(request.namespace());
        entity.setSettingKey(request.settingKey());
        entity.setSettingValue(request.settingValue());
        entity.setValueType(request.valueType());
        entity.setLevel(request.level());
        entity.setTenantId(request.tenantId() != null ? request.tenantId().value() : null);
        entity.setOutletId(request.outletId() != null ? request.outletId().value() : null);
        entity.setTerminalId(request.terminalId() != null ? request.terminalId().value() : null);
        entity.setActive(true);
        return entity;
    }

    /**
   * Update an existing setting.
   *
   * @param id setting ID
   * @param request update request
   * @return updated setting view
   * @throws IllegalArgumentException if not found or validation fails
   */
  @Transactional
  @CacheEvict(cacheNames = SettingsCacheNames.RESOLVED_SETTINGS, allEntries = true)
  public SettingView update(SettingId id, UpdateSettingRequest request) {
    log.info("Updating setting ID: {}", id);

    SettingEntity entity =
        repository
            .findById(id.value())
            .orElseThrow(() -> new IllegalArgumentException("Setting not found: " + id));

    // Update value (if provided)
    if (request.settingValue() != null) {
      // Validate new value
      SettingsValidator.validateOrThrow(
          entity.getNamespace(),
          entity.getSettingKey(),
          entity.getValueType(),
          request.settingValue());
      entity.setSettingValue(request.settingValue());
    }

    // Update active status (if provided)
    if (request.active() != null) {
      entity.setActive(request.active());
    }

    entity = repository.save(entity);
    log.info("Updated setting: {}", entity.fullKey());

    return mapper.toView(entity);
  }

  /**
   * Delete a setting (soft delete).
   *
   * @param id setting ID
   * @throws IllegalArgumentException if not found
   */
  @Transactional
  @CacheEvict(cacheNames = SettingsCacheNames.RESOLVED_SETTINGS, allEntries = true)
  public void delete(SettingId id) {
    log.info("Deleting setting ID: {}", id);

    SettingEntity entity =
        repository
            .findById(id.value())
            .orElseThrow(() -> new IllegalArgumentException("Setting not found: " + id));

    // Soft delete
    entity.setDeletedAt(Instant.now());
    entity.setActive(false);
    repository.save(entity);

    log.info("Soft-deleted setting: {}", entity.fullKey());
  }

  // ========================================
  // Validation helpers
  // ========================================

  private void checkUniqueness(CreateSettingRequest request) {
    UUID tenantId = request.tenantId() != null ? request.tenantId().value() : null;
    UUID outletId = request.outletId() != null ? request.outletId().value() : null;
    UUID terminalId = request.terminalId() != null ? request.terminalId().value() : null;

    var existing =
        repository
            .findFirstByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletIdAndTerminalIdAndNamespaceAndSettingKey(
                request.level(),
                tenantId,
                outletId,
                terminalId,
                request.namespace(),
                request.settingKey());

    if (existing.isPresent()) {
      throw new IllegalArgumentException(
          "Setting already exists: "
              + request.namespace()
              + "."
              + request.settingKey()
              + " at level "
              + request.level());
    }
  }

  private void validateLevelRequirements(CreateSettingRequest request) {
    switch (request.level()) {
      case GLOBAL -> {
        if (request.tenantId() != null
            || request.outletId() != null
            || request.terminalId() != null) {
          throw new IllegalArgumentException("GLOBAL level must not have tenant/outlet/terminal");
        }
      }
      case TENANT -> {
        if (request.tenantId() == null) {
          throw new IllegalArgumentException("TENANT level requires tenantId");
        }
        if (request.outletId() != null || request.terminalId() != null) {
          throw new IllegalArgumentException("TENANT level must not have outlet/terminal");
        }
      }
      case OUTLET -> {
        if (request.tenantId() == null || request.outletId() == null) {
          throw new IllegalArgumentException("OUTLET level requires tenantId and outletId");
        }
        if (request.terminalId() != null) {
          throw new IllegalArgumentException("OUTLET level must not have id");
        }
      }
      case TERMINAL -> {
        if (request.tenantId() == null || request.terminalId() == null) {
          throw new IllegalArgumentException("TERMINAL level requires tenantId and id");
        }
      }
    }
  }

  // ========================================
  // Search specification builder
  // ========================================

  private Specification<SettingEntity> buildSpecification(SearchSettingsCriteria criteria) {
    Specification<SettingEntity> spec = (root, query, cb) -> cb.conjunction();

    // Always filter by active and non-deleted
    spec = spec.and((root, query, cb) -> cb.isTrue(root.get("active")));
    spec = spec.and((root, query, cb) -> cb.isNull(root.get("deletedAt")));

    if (criteria.namespace() != null) {
      spec =
          spec.and(
              (root, query, cb) -> cb.equal(root.get("namespace"), criteria.namespace()));
    }

    if (criteria.settingKey() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.like(
                      cb.lower(root.get("settingKey")),
                      "%" + criteria.settingKey().toLowerCase() + "%"));
    }

    if (criteria.level() != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("level"), criteria.level()));
    }

    if (criteria.tenantId() != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.equal(root.get("tenantId"), criteria.tenantId().value()));
    }

    if (criteria.active() != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), criteria.active()));
    }

    return spec;
  }
}
