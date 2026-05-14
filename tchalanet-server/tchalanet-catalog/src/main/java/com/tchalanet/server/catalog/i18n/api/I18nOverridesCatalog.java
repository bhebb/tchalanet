package com.tchalanet.server.catalog.i18n.api;

import com.tchalanet.server.catalog.i18n.api.model.I18nGlobalKeyStatsView;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideView;
import com.tchalanet.server.catalog.i18n.api.model.SearchI18nOverridesCriteria;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;

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

    TchPage<I18nOverrideView> search(
        SearchI18nOverridesCriteria criteria, TchPageRequest pageRequest);

    /**
     * Find override by key.
     *
     * <p>Find a specific i18n override by tenant, locale, and key.
     *
     * @param locale  locale code (required, e.g., "fr", "en", "ht")
     * @param i18nKey translation key (required, e.g., "common.save")
     * @return override view if found
     */
    Optional<I18nOverrideView> findByKey(String locale, String i18nKey);


    /**
     * Resolve effective overrides for a locale.
     * Returns a map where tenant overrides overwrite global ones.
     * <p>
     * key = i18n_key
     * value = i18n_value
     */
    Map<String, String> resolveLocale(String locale, TchRequestContext ctx);

    /**
     * Convenience: resolve GLOBAL-only (platform scope) when no context is available.
     */
    default Map<String, String> resolveLocale(String locale) {
        return resolveLocale(locale, null);
    }

    /**
     * Convenience: resolve merged GLOBAL + TENANT overrides for the specified tenant.
     */
    Map<String, String> resolveLocaleForTenant(String locale, TenantId tenantId);

    // NEW: platform global stats for i18n keys/overrides
    I18nGlobalKeyStatsView keyStats();

}
