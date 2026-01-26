package com.tchalanet.server.catalog.i18n.api;

import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideView;
import com.tchalanet.server.catalog.i18n.api.model.SearchI18nOverridesCriteria;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
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
     * Get overrides as a map (key → value) for a tenant and locale.
     *
     * <p>Convenience method for direct translation lookups.
     *
     * @param tenantId tenant ID (required)
     * @param locale   locale code (required)
     * @return map of i18nKey → i18nValue
     */
    /**
     * Resolve effective overrides for a locale.
     * Returns a map where tenant overrides overwrite global ones.
     * <p>
     * key = i18n_key
     * value = i18n_value
     */
    Map<String, String> resolveLocale(String locale, @CurrentContext TchRequestContext ctx);
}
