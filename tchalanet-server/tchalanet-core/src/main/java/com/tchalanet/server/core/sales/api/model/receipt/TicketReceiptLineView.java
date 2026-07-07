package com.tchalanet.server.core.sales.api.model.receipt;

import java.math.BigDecimal;
import java.util.Objects;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.coverage.PotentialGainMode;

public record TicketReceiptLineView(
    int lineNo,
    String gameCode,
    String betType,
    Short betOption,
    String optionLabel,
    String gameLabel,
    String selection,
    BigDecimal odds,
    Money stake,
    Money potentialPayout,
    PotentialGainMode potentialGainMode,
    Money minPotentialPayout,
    Money maxPotentialPayout,
    Money totalPotentialPayout,
    boolean promotional,
    String promotionLabel,
    String promotionEffectType
) {
    public TicketReceiptLineView(
        int lineNo,
        String gameCode,
        String betType,
        Short betOption,
        String optionLabel,
        String gameLabel,
        String selection,
        BigDecimal odds,
        Money stake,
        Money potentialPayout,
        boolean promotional,
        String promotionLabel,
        String promotionEffectType
    ) {
        this(
            lineNo,
            gameCode,
            betType,
            betOption,
            optionLabel,
            gameLabel,
            selection,
            odds,
            stake,
            potentialPayout,
            PotentialGainMode.SINGLE,
            potentialPayout,
            potentialPayout,
            null,
            promotional,
            promotionLabel,
            promotionEffectType
        );
    }

    public TicketReceiptLineView {
        Objects.requireNonNull(gameCode, "gameCode is required");
        Objects.requireNonNull(betType, "betType is required");
        Objects.requireNonNull(selection, "selection is required");
        Objects.requireNonNull(stake, "stake is required");
        Objects.requireNonNull(potentialPayout, "potentialPayout is required");
        potentialGainMode = potentialGainMode == null ? PotentialGainMode.SINGLE : potentialGainMode;
        minPotentialPayout = minPotentialPayout == null ? potentialPayout : minPotentialPayout;
        maxPotentialPayout = maxPotentialPayout == null ? potentialPayout : maxPotentialPayout;
    }
}
