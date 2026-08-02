package com.tchalanet.server.features.tenantadmin.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameRefView;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantGamesPricingServiceTest {

  private final TenantGameApi tenantGameApi = mock(TenantGameApi.class);
  private final GameCatalog gameCatalog = mock(GameCatalog.class);
  private final QueryBus queryBus = mock(QueryBus.class);
  private final TenantGamesPricingService service =
      new TenantGamesPricingService(tenantGameApi, gameCatalog, queryBus);
  private final TenantId tenantId = TenantId.of(UUID.randomUUID());

  @AfterEach
  void clearResponseContext() {
    ApiResponseContext.clear();
  }

  @Test
  void keeps_games_visible_when_optional_setup_slices_fail() {
    var gameCode = "HT_BOLET";
    when(tenantGameApi.listGames(tenantId))
        .thenReturn(
            List.of(
                new TenantGameRefView(
                    TenantGameId.of(UUID.randomUUID()),
                    GameId.of(UUID.randomUUID()),
                    gameCode,
                    true,
                    true,
                    "Bolet",
                    1,
                    BigDecimal.ONE,
                    BigDecimal.TEN)));
    when(gameCatalog.findByCode(gameCode)).thenThrow(new IllegalStateException("catalog down"));
    doThrow(new IllegalStateException("limits down"))
        .when(queryBus)
        .ask(
            any(
                com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery
                    .class));
    doThrow(new IllegalStateException("pricing down"))
        .when(queryBus)
        .ask(any(com.tchalanet.server.core.pricing.api.query.ListTenantPricingRulesQuery.class));

    var view = service.get(tenantId);

    assertThat(view.games())
        .singleElement()
        .satisfies(
            game -> {
              assertThat(game.catalogName()).isEqualTo(gameCode);
              assertThat(game.limits().configured()).isFalse();
              assertThat(game.pricing().configured()).isFalse();
              assertThat(game.enabled()).isTrue();
            });
    assertThat(ApiResponseContext.get().getNotices())
        .extracting(notice -> notice.code())
        .containsExactlyInAnyOrder(
            "tenantadmin.games.catalog_unavailable",
            "tenantadmin.games.limits_unavailable",
            "tenantadmin.games.pricing_unavailable");
  }
}
