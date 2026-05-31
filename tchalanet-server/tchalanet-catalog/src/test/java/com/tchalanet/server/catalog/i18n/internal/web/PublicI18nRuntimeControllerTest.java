package com.tchalanet.server.catalog.i18n.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.api.model.I18nBundleView;
import com.tchalanet.server.catalog.i18n.api.model.I18nSurface;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

class PublicI18nRuntimeControllerTest {

    private final I18nOverridesCatalog catalog = mock(I18nOverridesCatalog.class);
    private final PublicI18nRuntimeController controller =
        new PublicI18nRuntimeController(catalog);

    @Test
    void validPublicSurfacesReturnBundle() {
        var bundle = new I18nBundleView("fr", Map.of(
            I18nSurface.PUBLIC_HOME, Map.of("home.title", "Bienvenue")));
        when(catalog.loadBundle(eq("fr"), any())).thenReturn(bundle);

        var response = controller.getBundle("fr", List.of(I18nSurface.PUBLIC_HOME));

        assertThat(response.data().locale()).isEqualTo("fr");
        assertThat(response.data().surfaces()).containsKey(I18nSurface.PUBLIC_HOME);
    }

    @Test
    void privateSurfaceCauses400() {
        assertThatThrownBy(
            () -> controller.getBundle("fr", List.of(I18nSurface.TENANT_ADMIN)))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(catalog, never()).loadBundle(any(), any());
    }

    @Test
    void mixedPublicAndPrivateSurfaceCauses400() {
        assertThatThrownBy(
            () -> controller.getBundle("fr",
                List.of(I18nSurface.PUBLIC_HOME, I18nSurface.CASHIER)))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(catalog, never()).loadBundle(any(), any());
    }

    @Test
    void emptySurfaceListCauses400() {
        assertThatThrownBy(() -> controller.getBundle("fr", List.of()))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void allPublicSurfacesAreAccepted() {
        when(catalog.loadBundle(any(), any()))
            .thenReturn(new I18nBundleView("fr", Map.of()));

        var allPublic = List.of(
            I18nSurface.PUBLIC_HOME,
            I18nSurface.PUBLIC_RESULTS,
            I18nSurface.PUBLIC_TICKET_CHECK,
            I18nSurface.COMMON_PUBLIC_ERROR);

        var response = controller.getBundle("fr", allPublic);
        assertThat(response.data()).isNotNull();
    }

    @Test
    void internalSurfaceCauses400() {
        assertThatThrownBy(
            () -> controller.getBundle("fr", List.of(I18nSurface.INTERNAL)))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void catalogIsCalledWithExactlyRequestedSurfaces() {
        when(catalog.loadBundle(any(), any()))
            .thenReturn(new I18nBundleView("fr", Map.of()));

        controller.getBundle("fr",
            List.of(I18nSurface.PUBLIC_HOME, I18nSurface.PUBLIC_RESULTS));

        verify(catalog).loadBundle("fr",
            Set.of(I18nSurface.PUBLIC_HOME, I18nSurface.PUBLIC_RESULTS));
    }
}
