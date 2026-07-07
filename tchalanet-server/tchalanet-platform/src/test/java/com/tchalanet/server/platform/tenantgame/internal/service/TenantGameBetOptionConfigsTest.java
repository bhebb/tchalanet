package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import com.tchalanet.server.platform.tenantgame.api.model.TenantBetOptionConfig;
import com.tchalanet.server.platform.tenantgame.api.model.TenantBetTypeOptionConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantGameBetOptionConfigsTest {

    private final TenantGameBetOptionConfigs configs = new TenantGameBetOptionConfigs();

    @Test
    void defaultsUseCatalogBetOptionsAndExplicitPolicy() {
        var defaults = configs.defaultsFor("HT_LOTO3");

        assertThat(defaults).singleElement().satisfies(config -> {
            assertThat(config.betType()).isEqualTo(BetType.LOTTO3_3D);
            assertThat(config.selectionPolicy()).isEqualTo(SelectionPolicy.EXPLICIT_ONLY);
            assertThat(config.defaultOption()).isEqualTo((short) 1);
            assertThat(config.options()).extracting(TenantBetOptionConfig::code)
                .containsExactly((short) 1, (short) 2);
        });
    }

    @Test
    void normalizeRejectsUnsupportedBetTypeForGame() {
        var requested = List.of(new TenantBetTypeOptionConfig(
            BetType.LOTTO5_PATTERN,
            SelectionPolicy.EXPLICIT_ONLY,
            (short) 1,
            List.of(new TenantBetOptionConfig((short) 1, true, true, 1))));

        assertThatThrownBy(() -> configs.normalize("HT_LOTO3", requested))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported betType");
    }

    @Test
    void normalizeRejectsDisabledDefaultOption() {
        var requested = List.of(new TenantBetTypeOptionConfig(
            BetType.LOTTO3_3D,
            SelectionPolicy.EXPLICIT_ONLY,
            (short) 2,
            List.of(
                new TenantBetOptionConfig((short) 1, true, true, 1),
                new TenantBetOptionConfig((short) 2, false, true, 2))));

        assertThatThrownBy(() -> configs.normalize("HT_LOTO3", requested))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("defaultOption");
    }

    @Test
    void normalizeRejectsImplicitBestMatchInV0() {
        var requested = List.of(new TenantBetTypeOptionConfig(
            BetType.LOTTO3_3D,
            SelectionPolicy.IMPLICIT_BEST_MATCH,
            (short) 1,
            List.of(
                new TenantBetOptionConfig((short) 1, true, true, 1),
                new TenantBetOptionConfig((short) 2, true, true, 2))));

        assertThatThrownBy(() -> configs.normalize("HT_LOTO3", requested))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("IMPLICIT_BEST_MATCH is disabled in V0");
    }
}
