package com.tchalanet.server.catalog.i18n.api.model;

/**
 * Search I18n Overrides Criteria
 *
 * <p>Search criteria for paginated search of i18n overrides.
 *
 * @param locale          filter by locale (optional, exact match)
 * @param i18nKeyContains filter by key (optional, contains match)
 * @param active          filter by active status (optional, null = all)
 */
public record SearchI18nOverridesCriteria(
    String locale,
    String i18nKeyContains,
    Boolean active,
    I18nOverrideLevel level // optional
) {
    public static SearchI18nOverridesCriteria empty() {
        return new SearchI18nOverridesCriteria(null, null, null, null);
    }
}

