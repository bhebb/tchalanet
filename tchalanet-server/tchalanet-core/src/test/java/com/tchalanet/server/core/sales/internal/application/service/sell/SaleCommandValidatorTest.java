package com.tchalanet.server.core.sales.internal.application.service.sell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.ChannelGamesView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelCalendarRow;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelGameView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSearchCriteria;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSummaryView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.catalog.game.api.model.GameStatsView;
import com.tchalanet.server.catalog.game.api.model.GameSummaryView;
import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.common.types.id.DrawChannelGameId;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionValidationResult;
import com.tchalanet.server.core.selection.internal.application.DefaultSelectionApi;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SaleCommandValidator — unsupported bet option is rejected at sale")
class SaleCommandValidatorTest {

  private static final CurrencyCode HTG = CurrencyCode.of("HTG");
  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("70000000-0000-0000-0000-000000000001"));
  private static final TenantGameId TENANT_GAME_ID =
      TenantGameId.of(UUID.fromString("71000000-0000-0000-0000-000000000001"));

  // SelectionApi is never reached for these cases (bet option validation throws first).
  private final SaleCommandValidator validator =
      new SaleCommandValidator(
          new UnreachableSelectionApi(),
          TenantGameApiStub.explicitOnly(true, true),
          DrawChannelCatalogStub.enabled(),
          GameCatalogStub.active());

  @Test
  @DisplayName("unsupported option code is rejected with bet_option_out_of_range")
  void unsupportedOptionRejected() {
    var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 99, "1234"));

    assertThatThrownBy(() -> validator.validateCommand(command))
        .isInstanceOf(ProblemRestException.class)
        .extracting(ex -> ((ProblemRestException) ex).getProblem().getProperties().get("code"))
        .isEqualTo("sales.bet_option_out_of_range");
  }

  @Test
  @DisplayName("missing required option is rejected with bet_option_required")
  void missingRequiredOptionRejected() {
    var validator =
        new SaleCommandValidator(
            new PassingSelectionApi(),
            TenantGameApiStub.explicitOnly(true, true),
            DrawChannelCatalogStub.enabled(),
            GameCatalogStub.active());
    var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, null, "1234"));

    validator.validateCommand(command);
    assertThatThrownBy(() -> validator.validateTenantConfiguration(command, TENANT_ID))
        .isInstanceOf(ProblemRestException.class)
        .extracting(ex -> ((ProblemRestException) ex).getProblem().getProperties().get("code"))
        .isEqualTo("sales.bet_option_required");
  }

  @Test
  @DisplayName("option supplied for an option-less bet type is rejected")
  void optionNotAllowedRejected() {
    var command = command(line(GameCode.HT_BOLET, BetType.MATCH_1_2D, (short) 1, "36"));

    assertThatThrownBy(() -> validator.validateCommand(command))
        .isInstanceOf(ProblemRestException.class)
        .extracting(ex -> ((ProblemRestException) ex).getProblem().getProperties().get("code"))
        .isEqualTo("sales.bet_option_not_allowed");
  }

  @Test
  @DisplayName("a supported option passes bet option validation")
  void supportedOptionPasses() {
    // Reaches selection validation (stub returns a canonical selection) then succeeds.
    var validator =
        new SaleCommandValidator(
            new PassingSelectionApi(),
            TenantGameApiStub.explicitOnly(true, true),
            DrawChannelCatalogStub.enabled(),
            GameCatalogStub.active());
    var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1234"));

    assertThat(command.lines()).hasSize(1);
    validator.validateCommand(command);
  }

  @Test
  @DisplayName("explicit-only hidden tenant option is rejected")
  void explicitOnlyHiddenOptionRejected() {
    var validator =
        new SaleCommandValidator(
            new PassingSelectionApi(),
            TenantGameApiStub.explicitOnly(true, false),
            DrawChannelCatalogStub.enabled(),
            GameCatalogStub.active());
    var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1234"));

    validator.validateCommand(command);
    assertThatThrownBy(() -> validator.validateTenantConfiguration(command, TENANT_ID))
        .isInstanceOf(ProblemRestException.class)
        .extracting(ex -> ((ProblemRestException) ex).getProblem().getProperties().get("code"))
        .isEqualTo("sales.tenant_bet_option_not_visible_in_pos");
  }

  @Test
  @DisplayName("implicit-best-match accepts a missing client option")
  void implicitBestMatchAcceptsMissingOption() {
    var validator =
        new SaleCommandValidator(
            new DefaultSelectionApi(),
            TenantGameApiStub.implicitBestMatch(),
            DrawChannelCatalogStub.enabled(),
            GameCatalogStub.active());
    var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, null, "1234"));

    validator.validateCommand(command);
    validator.validateTenantConfiguration(command, TENANT_ID);
  }

  @Test
  @DisplayName("implicit-best-match rejects invalid selections using the effective option")
  void implicitBestMatchRejectsInvalidSelection() {
    var validator =
        new SaleCommandValidator(
            new DefaultSelectionApi(),
            TenantGameApiStub.implicitBestMatch(),
            DrawChannelCatalogStub.enabled(),
            GameCatalogStub.active());
    var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, null, "12"));

    validator.validateCommand(command);
    assertThatThrownBy(() -> validator.validateTenantConfiguration(command, TENANT_ID))
        .isInstanceOf(ProblemRestException.class)
        .extracting(ex -> ((ProblemRestException) ex).getProblem().getProperties().get("code"))
        .isEqualTo("sales.selection_invalid");
  }

  @Test
  @DisplayName("implicit-best-match rejects a client supplied option")
  void implicitBestMatchRejectsClientOption() {
    var validator =
        new SaleCommandValidator(
            new PassingSelectionApi(),
            TenantGameApiStub.implicitBestMatch(),
            DrawChannelCatalogStub.enabled(),
            GameCatalogStub.active());
    var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1234"));

    validator.validateCommand(command);
    assertThatThrownBy(() -> validator.validateTenantConfiguration(command, TENANT_ID))
        .isInstanceOf(ProblemRestException.class)
        .extracting(ex -> ((ProblemRestException) ex).getProblem().getProperties().get("code"))
        .isEqualTo("sales.bet_option_not_allowed");
  }

  @Test
  @DisplayName("game not enabled on selected draw channel is rejected")
  void gameNotEnabledOnDrawChannelRejected() {
    var validator =
        new SaleCommandValidator(
            new PassingSelectionApi(),
            TenantGameApiStub.explicitOnly(true, true),
            DrawChannelCatalogStub.disabled(),
            GameCatalogStub.active());
    var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1234"));

    validator.validateCommand(command);
    assertThatThrownBy(() -> validator.validateTenantConfiguration(command, TENANT_ID))
        .isInstanceOf(ProblemRestException.class)
        .extracting(ex -> ((ProblemRestException) ex).getProblem().getProperties().get("code"))
        .isEqualTo("sales.game_not_available_on_draw_channel");
  }

  @Test
  @DisplayName("inactive platform game is rejected even when tenant game is enabled")
  void inactivePlatformGameRejected() {
    var validator =
        new SaleCommandValidator(
            new PassingSelectionApi(),
            TenantGameApiStub.explicitOnly(true, true),
            DrawChannelCatalogStub.enabled(),
            GameCatalogStub.inactive());
    var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1234"));

    validator.validateCommand(command);
    assertThatThrownBy(() -> validator.validateTenantConfiguration(command, TENANT_ID))
        .isInstanceOf(ProblemRestException.class)
        .extracting(ex -> ((ProblemRestException) ex).getProblem().getProperties().get("code"))
        .isEqualTo("sales.game_inactive");
  }

  private static SellTicketCommand command(SellTicketLineInput line) {
    return new SellTicketCommand(
        DrawId.of(UUID.fromString("80000000-0000-0000-0000-000000000001")),
        DrawChannelId.of(UUID.fromString("90000000-0000-0000-0000-000000000001")),
        HTG,
        List.of(line),
        null,
        List.of());
  }

  private static SellTicketLineInput line(
      GameCode gameCode, BetType betType, Short betOption, String rawSelection) {
    return new SellTicketLineInput(
        1, gameCode, betType, rawSelection, betOption, new BigDecimal("10"));
  }

  private static final class UnreachableSelectionApi implements SelectionApi {
    @Override
    public Selection canonicalize(BetType betType, Short betOption, String rawSelection) {
      throw new AssertionError("selection validation should not be reached");
    }

    @Override
    public Selection canonicalize(BetType betType, String rawSelection) {
      throw new AssertionError("selection validation should not be reached");
    }

    @Override
    public SelectionValidationResult validate(
        BetType betType, Short betOption, String rawSelection) {
      throw new AssertionError("selection validation should not be reached");
    }

    @Override
    public SelectionValidationResult validate(BetType betType, String rawSelection) {
      throw new AssertionError("selection validation should not be reached");
    }
  }

  private static final class PassingSelectionApi implements SelectionApi {
    @Override
    public Selection canonicalize(BetType betType, Short betOption, String rawSelection) {
      return new Selection(SelectionKeyValue(rawSelection), rawSelection);
    }

    @Override
    public Selection canonicalize(BetType betType, String rawSelection) {
      return canonicalize(betType, null, rawSelection);
    }

    @Override
    public SelectionValidationResult validate(
        BetType betType, Short betOption, String rawSelection) {
      return null;
    }

    @Override
    public SelectionValidationResult validate(BetType betType, String rawSelection) {
      return null;
    }

    private static com.tchalanet.server.core.selection.api.model.SelectionKey SelectionKeyValue(
        String v) {
      return com.tchalanet.server.core.selection.api.model.SelectionKey.of(v);
    }
  }

  private static final class TenantGameApiStub implements TenantGameApi {

    private final SelectionPolicy selectionPolicy;
    private final boolean optionEnabled;
    private final boolean optionVisibleInPos;

    private TenantGameApiStub(
        SelectionPolicy selectionPolicy, boolean optionEnabled, boolean optionVisibleInPos) {
      this.selectionPolicy = selectionPolicy;
      this.optionEnabled = optionEnabled;
      this.optionVisibleInPos = optionVisibleInPos;
    }

    static TenantGameApiStub explicitOnly(boolean optionEnabled, boolean optionVisibleInPos) {
      return new TenantGameApiStub(
          SelectionPolicy.EXPLICIT_ONLY, optionEnabled, optionVisibleInPos);
    }

    static TenantGameApiStub implicitBestMatch() {
      return new TenantGameApiStub(SelectionPolicy.IMPLICIT_BEST_MATCH, true, true);
    }

    @Override
    public TenantGameBetOptionConfigView getBetOptionConfig(TenantId tenantId, String gameCode) {
      return new TenantGameBetOptionConfigView(
          gameCode,
          List.of(
              new TenantBetTypeOptionConfigView(
                  BetType.LOTTO4_PATTERN,
                  selectionPolicy,
                  null,
                  List.of(
                      new TenantBetOptionView(
                          (short) 2, "Box", "Box", optionEnabled, optionVisibleInPos, 1)))));
    }

    @Override
    public List<TenantGameRefView> listGames(TenantId tenantId) {
      return List.of(
          new TenantGameRefView(
              TENANT_GAME_ID,
              null,
              GameCode.HT_LOTO4.name(),
              true,
              true,
              "Loto 4",
              1,
              new BigDecimal("1"),
              new BigDecimal("100")));
    }

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
  }

  private static final class DrawChannelCatalogStub implements DrawChannelCatalog {

    private final boolean enabled;

    private DrawChannelCatalogStub(boolean enabled) {
      this.enabled = enabled;
    }

    static DrawChannelCatalogStub enabled() {
      return new DrawChannelCatalogStub(true);
    }

    static DrawChannelCatalogStub disabled() {
      return new DrawChannelCatalogStub(false);
    }

    @Override
    public List<DrawChannelGameView> listGamesByChannel(
        TenantId tenantId, DrawChannelId channelId) {
      return List.of(
          new DrawChannelGameView(
              DrawChannelGameId.of(UUID.fromString("72000000-0000-0000-0000-000000000001")),
              channelId,
              TENANT_GAME_ID,
              enabled,
              null));
    }

    @Override
    public List<DrawChannelSummaryView> listAll(TenantId tenantId, Boolean activeOnly) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<DrawChannelView> listAllFull(TenantId tenantId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long countActiveChannels(TenantId tenantId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DrawChannelView> findById(TenantId tenantId, DrawChannelId id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DrawChannelView> findByTenantAndCode(TenantId tenantId, String code) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<ChannelGamesView> listChannelGames(TenantId tenantId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<DrawChannelCalendarRow> listCalendarRows(
        TenantId tenantId, Boolean activeOnly, Boolean enabledOnly) {
      throw new UnsupportedOperationException();
    }

    @Override
    public TchPage<DrawChannelView> search(
        DrawChannelSearchCriteria criteria, TchPageRequest pageReq) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class GameCatalogStub implements GameCatalog {

    private final boolean active;

    private GameCatalogStub(boolean active) {
      this.active = active;
    }

    static GameCatalogStub active() {
      return new GameCatalogStub(true);
    }

    static GameCatalogStub inactive() {
      return new GameCatalogStub(false);
    }

    @Override
    public List<GameView> listActive() {
      if (!active) return List.of();
      return List.of(
          new GameView(
              GameId.of(UUID.fromString("73000000-0000-0000-0000-000000000001")),
              GameCode.HT_LOTO4.name(),
              "Loto 4",
              "HAITI",
              null,
              4,
              4,
              null,
              true,
              1,
              null,
              null));
    }

    @Override
    public Optional<GameView> findByCode(String code) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<GameView> findById(GameId id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public GameStatsView stats() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<GameSummaryView> listRecent(int limit) {
      throw new UnsupportedOperationException();
    }
  }
}
