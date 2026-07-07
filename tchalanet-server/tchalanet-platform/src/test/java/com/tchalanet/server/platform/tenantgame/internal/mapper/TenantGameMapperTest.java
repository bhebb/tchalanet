package com.tchalanet.server.platform.tenantgame.internal.mapper;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import com.tchalanet.server.platform.tenantgame.api.model.TenantBetOptionConfig;
import com.tchalanet.server.platform.tenantgame.api.model.TenantBetTypeOptionConfig;
import com.tchalanet.server.platform.tenantgame.internal.persistence.TenantGameJpaEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantGameMapperTest {

    private final TenantGameMapper mapper = new TenantGameMapper();

    @Test
    void mapsBetOptionConfigBetweenEntityAndDomain() {
        var entity = new TenantGameJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(UUID.randomUUID());
        entity.setGameId(UUID.randomUUID());
        entity.setGameCode("HT_LOTO3");
        entity.setBetOptionConfig(List.of(new TenantBetTypeOptionConfig(
            BetType.LOTTO3_3D,
            SelectionPolicy.EXPLICIT_WITH_AUTO_OPTION,
            (short) 1,
            List.of(new TenantBetOptionConfig((short) 1, true, true, 1)))));

        var domain = mapper.toDomain(entity);
        var copy = mapper.toEntity(domain);

        assertThat(copy.getBetOptionConfig()).singleElement().satisfies(config -> {
            assertThat(config.betType()).isEqualTo(BetType.LOTTO3_3D);
            assertThat(config.selectionPolicy()).isEqualTo(SelectionPolicy.EXPLICIT_WITH_AUTO_OPTION);
            assertThat(config.defaultOption()).isEqualTo((short) 1);
        });
    }
}
