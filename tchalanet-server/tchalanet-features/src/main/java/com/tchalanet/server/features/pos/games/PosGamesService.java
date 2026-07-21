package com.tchalanet.server.features.pos.games;

import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantBetOptionView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantBetTypeOptionConfigView;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PosGamesService {

  private final TenantGameApi tenantGameApi;

  public PosGamesService(TenantGameApi tenantGameApi) {
    this.tenantGameApi = tenantGameApi;
  }

  public List<PosGameOptionResponse> listAvailable(TenantId tenantId) {
    return tenantGameApi.listGames(tenantId).stream()
        .filter(game -> game.enabled() && game.visibleInPos())
        .filter(game -> !GameCode.HT_MARYAJ_GRATIS.name().equals(game.gameCode()))
        .sorted(Comparator.comparingInt(game -> game.displayOrder()))
        .flatMap(game -> toOptions(tenantId, game.gameCode()).stream())
        .toList();
  }

  private List<PosGameOptionResponse> toOptions(TenantId tenantId, String gameCode) {
    var game = GameCode.valueOf(gameCode);
    var config = tenantGameApi.getBetOptionConfig(tenantId, gameCode);
    return config.betTypes().stream()
        .sorted(Comparator.comparing(item -> item.betType().name()))
        .map(betTypeConfig -> option(game, gameLabel(game), betTypeConfig))
        .toList();
  }

  private PosGameOptionResponse option(
      GameCode gameCode, String gameLabel, TenantBetTypeOptionConfigView config) {
    var betType = config.betType();
    var selectionShape = selectionShape(betType);
    return new PosGameOptionResponse(
        gameCode,
        gameLabel,
        betType,
        betTypeLabel(betType),
        betType.requiresOption(),
        config.selectionPolicy(),
        posOptions(config).stream().map(option -> toPosOption(betType, option)).toList(),
        selectionHint(betType, config.selectionPolicy()),
        selectionShape.digits(),
        selectionShape.segments());
  }

  private PosBetOptionResponse toPosOption(BetType betType, TenantBetOptionView option) {
    var betOption = BetOption.from(betType, option.code());
    var selectionShape = selectionShape(betOption);
    return new PosBetOptionResponse(
        option.code(),
        option.label(),
        option.description(),
        optionSelectionHint(betOption),
        selectionShape.digits(),
        selectionShape.segments());
  }

  private List<TenantBetOptionView> posOptions(TenantBetTypeOptionConfigView config) {
    if (config.selectionPolicy() == SelectionPolicy.IMPLICIT_BEST_MATCH) {
      return List.of();
    }
    return config.options().stream()
        .filter(option -> option.enabled() && option.visibleInPos())
        .sorted(Comparator.comparingInt(TenantBetOptionView::displayOrder))
        .toList();
  }

  private String gameLabel(GameCode gameCode) {
    return switch (gameCode) {
      case HT_BOLET -> "Bolet";
      case HT_MARYAJ -> "Maryaj";
      case HT_MARYAJ_GRATIS -> "Maryaj gratis";
      case HT_LOTO3 -> "Loto 3";
      case HT_LOTO4 -> "Loto 4";
      case HT_LOTO5 -> "Loto 5";
    };
  }

  private String betTypeLabel(BetType betType) {
    return switch (betType) {
      case MATCH_1_2D -> "1er lot";
      case MATCH_2_2D -> "2e lot";
      case MATCH_3_2D -> "3e lot";
      case MARRIAGE_2D2D -> "Maryaj";
      case LOTTO3_3D -> "Loto 3";
      case LOTTO4_PATTERN -> "Loto 4";
      case LOTTO5_PATTERN -> "Loto 5";
    };
  }

  private String selectionHint(BetType betType, SelectionPolicy selectionPolicy) {
    if (selectionPolicy == SelectionPolicy.IMPLICIT_BEST_MATCH) {
      return switch (betType) {
        case LOTTO3_3D -> "3 chiffres, ex: 123";
        case LOTTO4_PATTERN -> "4 chiffres, ex: 1245";
        case LOTTO5_PATTERN -> "5 chiffres, ex: 12345";
        default -> selectionHint(betType, SelectionPolicy.EXPLICIT_ONLY);
      };
    }
    return switch (betType) {
      case MATCH_1_2D, MATCH_2_2D, MATCH_3_2D -> "2 chiffres, ex: 45";
      case MARRIAGE_2D2D -> "Deux numeros de 2 chiffres separes par - ou /, ex: 12-45";
      case LOTTO3_3D -> "Choisissez une option puis entrez 3 chiffres, ex: 123";
      case LOTTO4_PATTERN -> "Choisissez une option puis entrez le numero demande.";
      case LOTTO5_PATTERN -> "Choisissez une option puis entrez 5 chiffres, ex: 12345";
    };
  }

  private String optionSelectionHint(BetOption option) {
    return switch (option) {
      case MARRIAGE_EXACT_ORDER, MARRIAGE_REVERSE_ALLOWED ->
          "Deux numeros de 2 chiffres, ex: 12-45";
      case LOTTO3_STRAIGHT, LOTTO3_BOX, LOTTO3_EXACT_PLUS_BOX -> "3 chiffres, ex: 123";
      case LOTTO4_STRAIGHT, LOTTO4_BOX -> "4 chiffres, ex: 1245";
      case LOTTO4_EXACT_PLUS_BOX -> "4 chiffres, ex: 1245";
      case LOTTO4_FRONT_PAIR -> "2 chiffres, ex: 12";
      case LOTTO4_BACK_PAIR -> "2 chiffres, ex: 45";
      case LOTTO5_LOT1_LOT2, LOTTO5_LOT1_LOT3, LOTTO5_MIXED_1_2_3 -> "5 chiffres, ex: 12345";
    };
  }

  private SelectionShape selectionShape(BetType betType) {
    return switch (betType) {
      case MATCH_1_2D, MATCH_2_2D, MATCH_3_2D -> new SelectionShape(2, 1);
      case MARRIAGE_2D2D -> new SelectionShape(2, 2);
      case LOTTO3_3D -> new SelectionShape(3, 1);
      case LOTTO4_PATTERN -> new SelectionShape(4, 1);
      case LOTTO5_PATTERN -> new SelectionShape(5, 1);
    };
  }

  private SelectionShape selectionShape(BetOption option) {
    return switch (option) {
      case MARRIAGE_EXACT_ORDER, MARRIAGE_REVERSE_ALLOWED -> new SelectionShape(2, 2);
      case LOTTO4_FRONT_PAIR, LOTTO4_BACK_PAIR -> new SelectionShape(2, 1);
      case LOTTO3_STRAIGHT, LOTTO3_BOX, LOTTO3_EXACT_PLUS_BOX -> new SelectionShape(3, 1);
      case LOTTO4_STRAIGHT, LOTTO4_BOX, LOTTO4_EXACT_PLUS_BOX -> new SelectionShape(4, 1);
      case LOTTO5_LOT1_LOT2, LOTTO5_LOT1_LOT3, LOTTO5_MIXED_1_2_3 -> new SelectionShape(5, 1);
    };
  }

  private record SelectionShape(int digits, int segments) {}
}
