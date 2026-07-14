package com.tchalanet.server.core.sales.api.model.receipt;

import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import java.util.Objects;

public record TicketReceiptLineView(
    int lineNo,
    String gameCode,
    String betType,
    Short betOption,
    String optionLabel,
    String gameLabel,
    String selection,
    Money stake,
    SelectionPolicy selectionPolicySnapshot,
    boolean promotional,
    String promotionLabel,
    String promotionEffectType) {
  public TicketReceiptLineView {
    Objects.requireNonNull(gameCode, "gameCode is required");
    Objects.requireNonNull(betType, "betType is required");
    Objects.requireNonNull(selection, "selection is required");
    Objects.requireNonNull(stake, "stake is required");
  }
}
