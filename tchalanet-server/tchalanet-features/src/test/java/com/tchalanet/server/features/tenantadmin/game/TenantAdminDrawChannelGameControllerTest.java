package com.tchalanet.server.features.tenantadmin.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.internal.write.DrawChannelGameAdminService;
import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantAdminDrawChannelGameControllerTest {

  private final DrawChannelCatalog drawChannelCatalog = mock(DrawChannelCatalog.class);
  private final TenantGameApi tenantGameApi = mock(TenantGameApi.class);
  private final GameCatalog gameCatalog = mock(GameCatalog.class);
  private final DrawChannelGameAdminService admin = mock(DrawChannelGameAdminService.class);
  private final TenantAdminDrawChannelGameController controller =
      new TenantAdminDrawChannelGameController(
          drawChannelCatalog, tenantGameApi, gameCatalog, admin);

  private final TenantId tenantId = TenantId.of(UUID.randomUUID());
  private final DrawChannelId drawChannelId = DrawChannelId.of(UUID.randomUUID());
  private final TenantGameId tenantGameId = TenantGameId.of(UUID.randomUUID());

  @Test
  void offer_uses_catalog_descriptor_when_draw_channel_is_missing() {
    when(drawChannelCatalog.findById(tenantId, drawChannelId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                controller.offer(
                    drawChannelId,
                    tenantGameId,
                    new TenantAdminDrawChannelGameController.OfferChannelGameRequest(true, null),
                    context()))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(
            error ->
                assertThat(((ProblemRestException) error).getProblem().getProperties())
                    .containsEntry("code", "catalog.drawchannel.deleted"));
  }

  private TchRequestContext context() {
    return new TchRequestContext(
        "tenant-demo",
        tenantId.value(),
        "tenant-demo",
        tenantId.value(),
        UserId.of(UUID.randomUUID()).value(),
        Set.of(),
        Set.of(),
        Locale.FRANCE,
        "req-test",
        "127.0.0.1",
        null,
        false,
        null,
        "active",
        ApiScope.TENANT,
        null,
        tenantId,
        java.time.ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        null,
        TchActorType.APP_USER,
        null,
        Set.of(),
        Set.of(),
        null);
  }
}
