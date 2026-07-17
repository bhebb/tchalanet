package com.tchalanet.server.features.ops.sales;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.UUID;

class OpsSalesSimulationPlanner {

  List<PlannedTicket> plan(OpsSalesSimulationRequest request, BigDecimal stakeAmount, long seed) {
    var draws = request.drawIds();
    var sellers = request.sellerTerminalIds();
    var mix = request.ticketMix();
    var random = new SplittableRandom(seed);
    var out = new ArrayList<PlannedTicket>();

    for (UUID drawId : draws) {
      for (UUID sellerTerminalId : sellers) {
        for (SimulatedGameKind kind : SimulatedGameKind.values()) {
          for (int i = 0; i < mix.count(kind); i++) {
            var selection = selection(kind, random);
            out.add(
                new PlannedTicket(
                    drawId,
                    sellerTerminalId,
                    kind,
                    selection,
                    new SellTicketLineInput(
                        1,
                        kind.gameCode(),
                        kind.betType(),
                        selection,
                        kind.betOption(),
                        stakeAmount)));
          }
        }
      }
    }

    return out;
  }

  int requestedTickets(OpsSalesSimulationRequest request) {
    var mix = request.ticketMix();
    return request.drawIds().size() * request.sellerTerminalIds().size() * mix.total();
  }

  private String selection(SimulatedGameKind kind, SplittableRandom random) {
    return switch (kind) {
      case MARYAJ -> twoDigits(random) + "-" + twoDigits(random);
      case BOLET -> twoDigits(random);
      case LOTO3 -> digits(random, 3);
      case LOTO4 -> digits(random, 4);
      case LOTO5 -> digits(random, 5);
    };
  }

  private String twoDigits(SplittableRandom random) {
    return digits(random, 2);
  }

  private String digits(SplittableRandom random, int width) {
    int bound = (int) Math.pow(10, width);
    return String.format("%0" + width + "d", random.nextInt(bound));
  }

  record PlannedTicket(
      UUID drawId,
      UUID sellerTerminalId,
      SimulatedGameKind kind,
      String selection,
      SellTicketLineInput line) {

    GameCode gameCode() {
      return kind.gameCode();
    }

    BetType betType() {
      return kind.betType();
    }

    Short betOption() {
      return kind.betOption();
    }
  }
}
