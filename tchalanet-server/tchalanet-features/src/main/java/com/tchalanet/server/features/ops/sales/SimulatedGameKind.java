package com.tchalanet.server.features.ops.sales;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;

enum SimulatedGameKind {
  BOLET(GameCode.HT_BOLET, BetType.MATCH_1_2D, null, 2),
  MARYAJ(GameCode.HT_MARYAJ, BetType.MARRIAGE_2D2D, (short) 1, 2),
  LOTO3(GameCode.HT_LOTO3, BetType.LOTTO3_3D, (short) 1, 3),
  LOTO4(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 1, 4),
  LOTO5(GameCode.HT_LOTO5, BetType.LOTTO5_PATTERN, (short) 1, 5);

  private final GameCode gameCode;
  private final BetType betType;
  private final Short betOption;
  private final int width;

  SimulatedGameKind(GameCode gameCode, BetType betType, Short betOption, int width) {
    this.gameCode = gameCode;
    this.betType = betType;
    this.betOption = betOption;
    this.width = width;
  }

  GameCode gameCode() {
    return gameCode;
  }

  BetType betType() {
    return betType;
  }

  Short betOption() {
    return betOption;
  }

  int width() {
    return width;
  }
}
