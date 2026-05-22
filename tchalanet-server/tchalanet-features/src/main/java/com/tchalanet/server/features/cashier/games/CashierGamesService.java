package com.tchalanet.server.features.cashier.games;

import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CashierGamesService {

    public List<CashierGameOptionResponse> listAvailable() {
        return List.of(
            option(GameCode.HT_BOLET, "Bolet", BetType.MATCH_1_2D, "1er lot"),
            option(GameCode.HT_BOLET, "Bolet", BetType.MATCH_2_2D, "2e lot"),
            option(GameCode.HT_BOLET, "Bolet", BetType.MATCH_3_2D, "3e lot"),
            option(GameCode.HT_MARYAJ, "Maryaj", BetType.MARRIAGE_2D2D, "Maryaj"),
            option(GameCode.HT_LOTO3, "Loto 3", BetType.LOTTO3_3D, "Loto 3"),
            option(GameCode.HT_LOTO4, "Loto 4", BetType.LOTTO4_PATTERN, "Loto 4"),
            option(GameCode.HT_LOTO5, "Loto 5", BetType.LOTTO5_PATTERN, "Loto 5")
        );
    }

    private CashierGameOptionResponse option(
        GameCode gameCode,
        String gameLabel,
        BetType betType,
        String betTypeLabel
    ) {
        return new CashierGameOptionResponse(
            gameCode,
            gameLabel,
            betType,
            betTypeLabel,
            betType.requiresOption(),
            BetOption.allowedFor(betType).stream()
                .map(option -> new CashierBetOptionResponse(
                    option.code(),
                    option.label(),
                    option.description(),
                    optionSelectionHint(option)))
                .toList(),
            selectionHint(betType)
        );
    }

    private String selectionHint(BetType betType) {
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
            case MARRIAGE_EXACT_ORDER, MARRIAGE_REVERSE_ALLOWED -> "Deux numeros de 2 chiffres, ex: 12-45";
            case LOTTO3_STRAIGHT, LOTTO3_BOX -> "3 chiffres, ex: 123";
            case LOTTO4_STRAIGHT, LOTTO4_BOX -> "4 chiffres, ex: 1245";
            case LOTTO4_FRONT_PAIR -> "2 chiffres, ex: 12";
            case LOTTO4_BACK_PAIR -> "2 chiffres, ex: 45";
            case LOTTO5_LOT1_LOT2, LOTTO5_LOT1_LOT3, LOTTO5_MIXED_1_2_3 -> "5 chiffres, ex: 12345";
        };
    }
}
