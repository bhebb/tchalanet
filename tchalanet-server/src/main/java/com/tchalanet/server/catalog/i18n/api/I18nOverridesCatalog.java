package com.tchalanet.server.catalog.i18n.api;

import com.tchalanet.server.catalog.i18n.internal.web.model.SearchI18nOverridesCriteria;
import com.tchalanet.server.common.types.id.I18nOverrideId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * I18n Overrides Catalog - Read Contract
 *
 * <p>Provides read-only access to tenant-specific i18n translation overrides.
 *
 * <p>This is the ONLY public interface for reading i18n overrides. All consumers (core, features)
 * must use this API.
 *
 * @see I18nOverrideView
 */
public interface I18nOverridesCatalog {

    /**
     * Find override by ID.
     *
     * @param id override ID
     * @return override view if found
     */
    Optional<I18nOverrideView> findById(I18nOverrideId id);

    I18nOverrideView getById(I18nOverrideId id);

    TchPage<I18nOverrideView> search(
        SearchI18nOverridesCriteria criteria, TchPageRequest pageRequest);

    /**
     * Find override by key.
     *
     * <p>Find a specific i18n override by tenant, locale, and key.
     *
     * @param tenantId tenant ID (required)
     * @param locale   locale code (required, e.g., "fr", "en", "ht")
     * @param i18nKey  translation key (required, e.g., "common.save")
     * @return override view if found
     */
    Optional<I18nOverrideView> findByKey(TenantId tenantId, String locale, String i18nKey);

    /**
     * List active overrides for a tenant and locale.
     *
     * <p>This is the main method for loading tenant-specific translations.
     *
     * @param tenantId tenant ID (required)
     * @param locale   locale code (required, e.g., "fr", "en", "ht")
     * @return list of active overrides
     */
    List<I18nOverrideView> listByTenantAndLocale(TenantId tenantId, String locale);

    /**
     * Get overrides as a map (key → value) for a tenant and locale.
     *
     * <p>Convenience method for direct translation lookups.
     *
     * @param tenantId tenant ID (required)
     * @param locale   locale code (required)
     * @return map of i18nKey → i18nValue
     */
    Map<String, String> getOverridesMap(TenantId tenantId, String locale);

    /**
     * List all active overrides for a tenant (all locales).
     *
     * @param tenantId tenant ID (required)
     * @return list of active overrides across all locales
     */
    List<I18nOverrideView> listByTenant(TenantId tenantId);
}
