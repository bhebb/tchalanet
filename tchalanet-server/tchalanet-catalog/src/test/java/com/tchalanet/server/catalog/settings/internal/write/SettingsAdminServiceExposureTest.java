package com.tchalanet.server.catalog.settings.internal.write;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.settings.api.model.SettingExposure;
import com.tchalanet.server.catalog.settings.api.model.SettingView;
import com.tchalanet.server.catalog.settings.internal.mapper.SettingMapper;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingEntity;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingRepository;
import com.tchalanet.server.catalog.settings.internal.registry.SettingsValidator;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Unit tests for SettingsAdminService.listActiveByExposure().
 * Uses Mockito.mock() for all dependencies — decouple from record/entity field changes.
 */
class SettingsAdminServiceExposureTest {

    private final SettingRepository repository = mock(SettingRepository.class);
    private final SettingMapper mapper = mock(SettingMapper.class);
    private final SettingsValidator validator = mock(SettingsValidator.class);

    private final SettingsAdminService service =
        new SettingsAdminService(repository, mapper);

    @Test
    void listActiveByExposureReturnsOnlyPublicRuntimeSettings() {
        var entity = mock(SettingEntity.class);
        when(entity.getExposure()).thenReturn(SettingExposure.PUBLIC_RUNTIME);

        var view = mockView(SettingExposure.PUBLIC_RUNTIME, "ui.i18n", "default_locale");
        when(repository.findByActiveTrueAndDeletedAtIsNullAndExposure(SettingExposure.PUBLIC_RUNTIME))
            .thenReturn(List.of(entity));
        when(mapper.toViews(List.of(entity))).thenReturn(List.of(view));

        var result = service.listActiveByExposure(SettingExposure.PUBLIC_RUNTIME, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).exposure()).isEqualTo(SettingExposure.PUBLIC_RUNTIME);
    }

    @Test
    void listActiveByExposureWithNamespaceFiltersCorrectly() {
        var entity = mock(SettingEntity.class);
        var view = mockView(SettingExposure.PUBLIC_RUNTIME, "ui.i18n", "default_locale");

        when(repository.findByActiveTrueAndDeletedAtIsNullAndExposureAndNamespace(
            SettingExposure.PUBLIC_RUNTIME, "ui.i18n"))
            .thenReturn(List.of(entity));
        when(mapper.toViews(List.of(entity))).thenReturn(List.of(view));

        var result = service.listActiveByExposure(SettingExposure.PUBLIC_RUNTIME, "ui.i18n");

        assertThat(result).hasSize(1);
    }

    @Test
    void listActiveByExposureReturnsEmptyWhenNoneExist() {
        when(repository.findByActiveTrueAndDeletedAtIsNullAndExposure(SettingExposure.PUBLIC_RUNTIME))
            .thenReturn(List.of());
        when(mapper.toViews(List.of())).thenReturn(List.of());

        var result = service.listActiveByExposure(SettingExposure.PUBLIC_RUNTIME, null);

        assertThat(result).isEmpty();
    }

    @Test
    void listActiveByExposureForInternalWorksForBackendCallers() {
        var entity = mock(SettingEntity.class);
        var view = mockView(SettingExposure.INTERNAL, "pos.behavior", "require_open_session");

        when(repository.findByActiveTrueAndDeletedAtIsNullAndExposure(SettingExposure.INTERNAL))
            .thenReturn(List.of(entity));
        when(mapper.toViews(List.of(entity))).thenReturn(List.of(view));

        var result = service.listActiveByExposure(SettingExposure.INTERNAL, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).exposure()).isEqualTo(SettingExposure.INTERNAL);
    }

    // ---------------------------------------------------------------
    // Factory — mock SettingView. If the record gains new fields,
    // only this method changes.
    // ---------------------------------------------------------------

    private static SettingView mockView(SettingExposure exposure, String namespace, String key) {
        var view = mock(SettingView.class);
        when(view.exposure()).thenReturn(exposure);
        when(view.namespace()).thenReturn(namespace);
        when(view.settingKey()).thenReturn(key);
        return view;
    }
}
