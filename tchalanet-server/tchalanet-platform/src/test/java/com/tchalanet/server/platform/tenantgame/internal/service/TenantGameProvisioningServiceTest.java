package com.tchalanet.server.platform.tenantgame.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnsureTenantGamesRequest;
import com.tchalanet.server.platform.tenantgame.internal.persistence.TenantGamePersistenceAdapter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TenantGameProvisioningServiceTest {

  private final GameCatalog gameCatalog = mock(GameCatalog.class);
  private final TenantGamePersistenceAdapter persistence = mock(TenantGamePersistenceAdapter.class);
  private final TenantGameProvisioningService service =
      new TenantGameProvisioningService(gameCatalog, persistence, new TenantGameBetOptionConfigs());

  @Test
  void createsActivePosVisibleTenantGameWithSellableDefaults() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var game =
        new GameView(
            GameId.of(UUID.randomUUID()),
            "HT_BOLET",
            "Bòlèt",
            "HAITI",
            "2D",
            2,
            2,
            null,
            true,
            10,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"));

    when(gameCatalog.findByCode("HT_BOLET")).thenReturn(Optional.of(game));
    when(persistence.findByTenantIdAndGameCode(tenantId, "HT_BOLET")).thenReturn(Optional.empty());
    when(persistence.save(any(TenantGame.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.ensureTenantGame(
        EnsureTenantGamesRequest.builder().tenantId(tenantId).gameCode("ht_bolet").build());

    var captor = ArgumentCaptor.forClass(TenantGame.class);
    verify(persistence).save(captor.capture());
    assertThat(captor.getValue())
        .satisfies(
            saved -> {
              assertThat(saved.tenantId()).isEqualTo(tenantId);
              assertThat(saved.gameId()).isEqualTo(game.id());
              assertThat(saved.gameCode()).isEqualTo("HT_BOLET");
              assertThat(saved.enabled()).isTrue();
              assertThat(saved.visibleInPos()).isTrue();
              assertThat(saved.displayOrder()).isEqualTo(10);
              assertThat(saved.minStake()).isEqualByComparingTo(new BigDecimal("1.00"));
              assertThat(saved.maxStake()).isEqualByComparingTo(new BigDecimal("1000000.00"));
            });
  }

  @Test
  void pushesMaryajGratisAfterSellerSelectableGames() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var game =
        new GameView(
            GameId.of(UUID.randomUUID()),
            "HT_MARYAJ_GRATIS",
            "Maryaj Gratis",
            "HAITI",
            "2D2D",
            4,
            4,
            null,
            true,
            35,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"));

    when(gameCatalog.findByCode("HT_MARYAJ_GRATIS")).thenReturn(Optional.of(game));
    when(persistence.findByTenantIdAndGameCode(tenantId, "HT_MARYAJ_GRATIS"))
        .thenReturn(Optional.empty());
    when(persistence.save(any(TenantGame.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.ensureTenantGame(
        EnsureTenantGamesRequest.builder().tenantId(tenantId).gameCode("HT_MARYAJ_GRATIS").build());

    var captor = ArgumentCaptor.forClass(TenantGame.class);
    verify(persistence).save(captor.capture());
    assertThat(captor.getValue().displayOrder()).isEqualTo(999);
  }
}
