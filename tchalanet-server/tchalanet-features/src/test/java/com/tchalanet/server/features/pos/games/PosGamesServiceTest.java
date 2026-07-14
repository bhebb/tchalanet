package com.tchalanet.server.features.pos.games;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import com.tchalanet.server.platform.tenantgame.api.model.request.DisableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnsureTenantGamesRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameBetOptionConfigRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameSettingsRequest;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantBetOptionView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantBetTypeOptionConfigView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameBetOptionConfigView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameRefView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PosGamesServiceTest {

  @Test
  void availableHidesDisabledOrInvisibleTenantOptions() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var config =
        new TenantGameBetOptionConfigView(
            GameCode.HT_LOTO3.name(),
            List.of(
                new TenantBetTypeOptionConfigView(
                    BetType.LOTTO3_3D,
                    SelectionPolicy.EXPLICIT_ONLY,
                    (short) 1,
                    List.of(
                        new TenantBetOptionView((short) 1, "Exact", "Exact", true, true, 1),
                        new TenantBetOptionView((short) 2, "Box", "Box", false, true, 2)))));
    var tenantGameApi = new FakeTenantGameApi(config);

    var result = new PosGamesService(tenantGameApi).listAvailable(tenantId);

    assertThat(result)
        .singleElement()
        .satisfies(
            game -> {
              assertThat(game.gameCode()).isEqualTo(GameCode.HT_LOTO3);
              assertThat(game.options())
                  .singleElement()
                  .extracting(PosBetOptionResponse::code)
                  .isEqualTo((short) 1);
            });
  }

  @Test
  void availableHidesOptionsWhenSelectionPolicyIsImplicitBestMatch() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var config =
        new TenantGameBetOptionConfigView(
            GameCode.HT_LOTO3.name(),
            List.of(
                new TenantBetTypeOptionConfigView(
                    BetType.LOTTO3_3D,
                    SelectionPolicy.IMPLICIT_BEST_MATCH,
                    (short) 1,
                    List.of(
                        new TenantBetOptionView((short) 1, "Exact", "Exact", true, true, 1),
                        new TenantBetOptionView((short) 2, "Box", "Box", true, true, 2)))));
    var tenantGameApi = new FakeTenantGameApi(config);

    var result = new PosGamesService(tenantGameApi).listAvailable(tenantId);

    assertThat(result)
        .singleElement()
        .satisfies(
            game -> {
              assertThat(game.gameCode()).isEqualTo(GameCode.HT_LOTO3);
              assertThat(game.selectionPolicy()).isEqualTo(SelectionPolicy.IMPLICIT_BEST_MATCH);
              assertThat(game.options()).isEmpty();
            });
  }

  private record FakeTenantGameApi(TenantGameBetOptionConfigView config) implements TenantGameApi {

    @Override
    public EnableTenantGameResult enableTenantGame(EnableTenantGameRequest request) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DisableTenantGameResult disableTenantGame(DisableTenantGameRequest request) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void updateTenantGameSettings(UpdateTenantGameSettingsRequest request) {
      throw new UnsupportedOperationException();
    }

    @Override
    public TenantGameBetOptionConfigView getBetOptionConfig(TenantId tenantId, String gameCode) {
      return config;
    }

    @Override
    public TenantGameBetOptionConfigView updateBetOptionConfig(
        UpdateTenantGameBetOptionConfigRequest request) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void ensureTenantGame(EnsureTenantGamesRequest request) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TenantGameRefView> findByTenantGameId(
        TenantId tenantId, TenantGameId tenantGameId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<TenantGameRefView> listGames(TenantId tenantId) {
      return List.of(
          new TenantGameRefView(
              null, null, GameCode.HT_LOTO3.name(), true, true, null, 1, null, null));
    }
  }
}
