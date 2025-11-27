package com.tchalanet.server.ticket.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A Value Object representing a single line on a ticket. It is immutable.
 *
 * @param gameCode The code of the game for this line (e.g., "BORLETTE_3").
 * @param selection The player's number selection (e.g., "123", "12-34").
 * @param stake The amount of money wagered on this line.
 * @param oddsSnapshot The odds for this selection at the time of purchase.
 * @param potentialPayout The potential winnings for this line (stake * odds).
 */
public record TicketLine(
    String gameCode,
    String selection,
    BigDecimal stake,
    BigDecimal oddsSnapshot,
    BigDecimal potentialPayout) {
  public TicketLine {
    Objects.requireNonNull(gameCode, "Game code cannot be null");
    Objects.requireNonNull(selection, "Selection cannot be null");
    Objects.requireNonNull(stake, "Stake cannot be null");
    if (stake.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Stake must be positive.");
    }
  }
}
