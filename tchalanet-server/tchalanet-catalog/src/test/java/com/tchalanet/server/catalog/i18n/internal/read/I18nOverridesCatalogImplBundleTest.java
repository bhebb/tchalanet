package com.tchalanet.server.catalog.i18n.internal.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.i18n.api.model.I18nBundleView;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel;
import com.tchalanet.server.catalog.i18n.api.model.I18nSurface;
import com.tchalanet.server.catalog.i18n.internal.mapper.I18nOverrideMapper;
import com.tchalanet.server.catalog.i18n.internal.persistence.I18nOverrideEntity;
import com.tchalanet.server.catalog.i18n.internal.persistence.I18nOverrideRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

/**
 * Unit tests for I18nOverridesCatalogImpl.loadBundle().
 *
 * Uses Mockito.mock() for repository and entity to decouple from
 * field additions in I18nOverrideEntity.
 */
class I18nOverridesCatalogImplBundleTest {

    private final I18nOverrideRepository repository = mock(I18nOverrideRepository.class);
    private final I18nOverrideMapper mapper = mock(I18nOverrideMapper.class);

    private final I18nOverridesCatalogImpl catalog =
        new I18nOverridesCatalogImpl(repository, mapper);

    @BeforeEach
    void noGlobalNoTenant() {
        // default: empty for both levels, overridden per test
        when(repository.findByLocaleAndLevelAndSurfaceInAndActiveTrueAndDeletedAtIsNull(
            "fr", I18nOverrideLevel.GLOBAL, Set.of(I18nSurface.PUBLIC_HOME)))
            .thenReturn(List.of());
        when(repository.findByLocaleAndLevelAndSurfaceInAndActiveTrueAndDeletedAtIsNull(
            "fr", I18nOverrideLevel.TENANT, Set.of(I18nSurface.PUBLIC_HOME)))
            .thenReturn(List.of());
    }

    @Test
    void bundleGroupsKeysBySurface() {
        var entity = entityWith(I18nSurface.PUBLIC_HOME, "home.title", "Bienvenue");
        when(repository.findByLocaleAndLevelAndSurfaceInAndActiveTrueAndDeletedAtIsNull(
            "fr", I18nOverrideLevel.GLOBAL, Set.of(I18nSurface.PUBLIC_HOME)))
            .thenReturn(List.of(entity));

        I18nBundleView bundle = catalog.loadBundle("fr", Set.of(I18nSurface.PUBLIC_HOME));

        assertThat(bundle.locale()).isEqualTo("fr");
        assertThat(bundle.surfaces()).containsKey(I18nSurface.PUBLIC_HOME);
        assertThat(bundle.surfaces().get(I18nSurface.PUBLIC_HOME))
            .containsEntry("home.title", "Bienvenue");
    }

    @Test
    void tenantRowOverridesGlobalForSameKey() {
        var global = entityWith(I18nSurface.PUBLIC_HOME, "home.cta", "Acheter");
        var tenant = entityWith(I18nSurface.PUBLIC_HOME, "home.cta", "Vérifier");

        when(repository.findByLocaleAndLevelAndSurfaceInAndActiveTrueAndDeletedAtIsNull(
            "fr", I18nOverrideLevel.GLOBAL, Set.of(I18nSurface.PUBLIC_HOME)))
            .thenReturn(List.of(global));
        when(repository.findByLocaleAndLevelAndSurfaceInAndActiveTrueAndDeletedAtIsNull(
            "fr", I18nOverrideLevel.TENANT, Set.of(I18nSurface.PUBLIC_HOME)))
            .thenReturn(List.of(tenant));

        I18nBundleView bundle = catalog.loadBundle("fr", Set.of(I18nSurface.PUBLIC_HOME));

        assertThat(bundle.surfaces().get(I18nSurface.PUBLIC_HOME))
            .containsEntry("home.cta", "Vérifier");
    }

    @Test
    void multipleSurfacesReturnedGrouped() {
        var home = entityWith(I18nSurface.PUBLIC_HOME, "home.title", "Bienvenue");
        var results = entityWith(I18nSurface.PUBLIC_RESULTS, "results.title", "Résultats");

        var surfaces = Set.of(I18nSurface.PUBLIC_HOME, I18nSurface.PUBLIC_RESULTS);
        when(repository.findByLocaleAndLevelAndSurfaceInAndActiveTrueAndDeletedAtIsNull(
            "fr", I18nOverrideLevel.GLOBAL, surfaces))
            .thenReturn(List.of(home, results));
        when(repository.findByLocaleAndLevelAndSurfaceInAndActiveTrueAndDeletedAtIsNull(
            "fr", I18nOverrideLevel.TENANT, surfaces))
            .thenReturn(List.of());

        I18nBundleView bundle = catalog.loadBundle("fr", surfaces);

        assertThat(bundle.surfaces()).containsKeys(I18nSurface.PUBLIC_HOME, I18nSurface.PUBLIC_RESULTS);
        assertThat(bundle.surfaces().get(I18nSurface.PUBLIC_HOME)).containsEntry("home.title", "Bienvenue");
        assertThat(bundle.surfaces().get(I18nSurface.PUBLIC_RESULTS)).containsEntry("results.title", "Résultats");
    }

    @Test
    void sameKeyInDifferentSurfacesCoexist() {
        var home = entityWith(I18nSurface.PUBLIC_HOME, "common.title", "Bienvenue");
        var results = entityWith(I18nSurface.PUBLIC_RESULTS, "common.title", "Résultats");

        var surfaces = Set.of(I18nSurface.PUBLIC_HOME, I18nSurface.PUBLIC_RESULTS);
        when(repository.findByLocaleAndLevelAndSurfaceInAndActiveTrueAndDeletedAtIsNull(
            "fr", I18nOverrideLevel.GLOBAL, surfaces))
            .thenReturn(List.of(home, results));
        when(repository.findByLocaleAndLevelAndSurfaceInAndActiveTrueAndDeletedAtIsNull(
            "fr", I18nOverrideLevel.TENANT, surfaces))
            .thenReturn(List.of());

        I18nBundleView bundle = catalog.loadBundle("fr", surfaces);

        assertThat(bundle.surfaces().get(I18nSurface.PUBLIC_HOME))
            .containsEntry("common.title", "Bienvenue");
        assertThat(bundle.surfaces().get(I18nSurface.PUBLIC_RESULTS))
            .containsEntry("common.title", "Résultats");
    }

    @Test
    void emptySurfacesReturnsEmptyBundle() {
        I18nBundleView bundle = catalog.loadBundle("fr", Set.of());
        assertThat(bundle.surfaces()).isEmpty();
    }

    @Test
    void nullLocaleReturnsEmptyBundle() {
        I18nBundleView bundle = catalog.loadBundle(null, Set.of(I18nSurface.PUBLIC_HOME));
        assertThat(bundle.surfaces()).isEmpty();
    }

    // ---------------------------------------------------------------
    // Factory — mock entity, stub only what loadBundle needs.
    // If I18nOverrideEntity gains new fields, only this method changes.
    // ---------------------------------------------------------------

    private static I18nOverrideEntity entityWith(I18nSurface surface, String key, String value) {
        var e = mock(I18nOverrideEntity.class);
        when(e.getSurface()).thenReturn(surface);
        when(e.getI18nKey()).thenReturn(key);
        when(e.getI18nValue()).thenReturn(value);
        return e;
    }
}
