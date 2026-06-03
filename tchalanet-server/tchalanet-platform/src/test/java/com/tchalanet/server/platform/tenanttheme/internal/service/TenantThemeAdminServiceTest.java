package com.tchalanet.server.platform.tenanttheme.internal.service;

import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.ThemePresetId;
import com.tchalanet.server.platform.tenanttheme.api.model.ApplyTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.UpdateTenantThemeSettingsRequest;
import com.tchalanet.server.platform.tenanttheme.internal.persistence.TenantThemePersistenceAdapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TenantThemeAdminServiceTest {

    private static final TenantId TENANT = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private ThemeCatalog themeCatalog;
    private TenantThemePersistenceAdapter persistence;
    private ApplicationEventPublisher eventPublisher;
    private TenantThemeAdminService service;

    @BeforeEach
    void setUp() {
        themeCatalog = mock(ThemeCatalog.class);
        persistence = mock(TenantThemePersistenceAdapter.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new TenantThemeAdminService(themeCatalog, persistence, eventPublisher, FIXED_CLOCK);
    }

    private ThemePresetView activePreset(String code) {
        return new ThemePresetView(ThemePresetId.of(UUID.randomUUID()), code, null,
            null, null, true, false, Instant.now(), Instant.now());
    }

    private TenantTheme existingTheme() {
        return new TenantTheme(TENANT, "default-light", "SYSTEM", true, false, 1L, null,
            Instant.now(), Instant.now(), "system");
    }

    @Test
    void applyPresetSavesNewTheme() {
        when(themeCatalog.findByCode("modern-dark")).thenReturn(Optional.of(activePreset("modern-dark")));
        when(persistence.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(persistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() ->
            service.applyPreset(new ApplyTenantThemeRequest(TENANT, "modern-dark")));

        verify(persistence).save(argThat(t -> t.presetCode().equals("modern-dark")));
    }

    @Test
    void applyPresetRejectsInactivePreset() {
        var inactive = new ThemePresetView(ThemePresetId.of(UUID.randomUUID()), "old",
            null, null, null, false, false, Instant.now(), Instant.now());
        when(themeCatalog.findByCode("old")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.applyPreset(new ApplyTenantThemeRequest(TENANT, "old")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not active");
    }

    @Test
    void applyPresetRejectsUnknownPreset() {
        when(themeCatalog.findByCode("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applyPreset(new ApplyTenantThemeRequest(TENANT, "unknown")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void updateSettingsChangesDefaultMode() {
        when(persistence.findActiveByTenantId(TENANT)).thenReturn(Optional.of(existingTheme()));
        when(persistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() ->
            service.updateSettings(new UpdateTenantThemeSettingsRequest(TENANT, "DARK")));

        verify(persistence).save(argThat(t -> t.defaultMode().equals("DARK")));
    }

    @Test
    void updateSettingsRejectsInvalidMode() {
        assertThatThrownBy(() -> new UpdateTenantThemeSettingsRequest(TENANT, "PURPLE"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("LIGHT, DARK, or SYSTEM");
    }

    @Test
    void updateSettingsFailsWhenNoThemeApplied() {
        when(persistence.findActiveByTenantId(TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSettings(new UpdateTenantThemeSettingsRequest(TENANT, "DARK")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("apply a preset first");
    }

    @Test
    void updateSettingsIncrementsVersion() {
        when(persistence.findActiveByTenantId(TENANT)).thenReturn(Optional.of(existingTheme()));
        when(persistence.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateSettings(new UpdateTenantThemeSettingsRequest(TENANT, "LIGHT"));

        verify(persistence).save(argThat(t -> t.version() == 2L));
    }
}
