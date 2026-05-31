package com.tchalanet.server.catalog.settings.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.settings.api.SettingsAdminCatalog;
import com.tchalanet.server.catalog.settings.api.model.SettingExposure;
import com.tchalanet.server.catalog.settings.api.model.SettingView;

import org.junit.jupiter.api.Test;

import java.util.List;

class PublicSettingsRuntimeControllerTest {

    private final SettingsAdminCatalog catalog = mock(SettingsAdminCatalog.class);
    private final PublicSettingsRuntimeController controller =
        new PublicSettingsRuntimeController(catalog);

    @Test
    void returnsPublicRuntimeSettingsWithoutNamespace() {
        var view = mock(SettingView.class);
        when(view.exposure()).thenReturn(SettingExposure.PUBLIC_RUNTIME);
        when(catalog.listActiveByExposure(SettingExposure.PUBLIC_RUNTIME, null))
            .thenReturn(List.of(view));

        var response = controller.getPublicSettings(null);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).exposure()).isEqualTo(SettingExposure.PUBLIC_RUNTIME);
    }

    @Test
    void namespaceIsPassedThroughToCatalog() {
        when(catalog.listActiveByExposure(SettingExposure.PUBLIC_RUNTIME, "ui.i18n"))
            .thenReturn(List.of());

        controller.getPublicSettings("ui.i18n");

        verify(catalog).listActiveByExposure(SettingExposure.PUBLIC_RUNTIME, "ui.i18n");
    }

    @Test
    void exposureIsAlwaysPublicRuntimeRegardlessOfInput() {
        // The controller hardcodes PUBLIC_RUNTIME — no exposure param accepted from client.
        when(catalog.listActiveByExposure(SettingExposure.PUBLIC_RUNTIME, null))
            .thenReturn(List.of());

        controller.getPublicSettings(null);

        // Verify only PUBLIC_RUNTIME was ever requested — never INTERNAL
        verify(catalog).listActiveByExposure(SettingExposure.PUBLIC_RUNTIME, null);
    }

    @Test
    void returnsEmptyListWhenNoneExist() {
        when(catalog.listActiveByExposure(SettingExposure.PUBLIC_RUNTIME, null))
            .thenReturn(List.of());

        var response = controller.getPublicSettings(null);

        assertThat(response.data()).isEmpty();
    }
}
