package com.tchalanet.server.catalog.game.api.model;


import java.util.Arrays;
import java.util.List;

public enum BetOption {

    // MARYAJ
    MARRIAGE_EXACT_ORDER(
        BetType.MARRIAGE_2D2D,
        (short) 1,
        "Ordre exact",
        "Les deux numéros doivent sortir dans l'ordre joué"
    ),

    MARRIAGE_REVERSE_ALLOWED(
        BetType.MARRIAGE_2D2D,
        (short) 2,
        "Revers / Double",
        "Les deux numéros peuvent sortir dans les deux sens"
    ),

    // LOTO 3
    LOTTO3_STRAIGHT(
        BetType.LOTTO3_3D,
        (short) 1,
        "Exact",
        "Les 3 chiffres doivent sortir exactement dans l'ordre joué"
    ),

    LOTTO3_BOX(
        BetType.LOTTO3_3D,
        (short) 2,
        "Désordre / Box",
        "Les 3 chiffres peuvent sortir dans n'importe quel ordre"
    ),

    // LOTO 4
    LOTTO4_STRAIGHT(
        BetType.LOTTO4_PATTERN,
        (short) 1,
        "Exact",
        "Les 4 chiffres doivent sortir exactement dans l'ordre joué"
    ),

    LOTTO4_BOX(
        BetType.LOTTO4_PATTERN,
        (short) 2,
        "Désordre / Box",
        "Les 4 chiffres peuvent sortir dans n'importe quel ordre"
    ),

    LOTTO4_FRONT_PAIR(
        BetType.LOTTO4_PATTERN,
        (short) 3,
        "2 premiers chiffres",
        "Les 2 premiers chiffres du tirage doivent correspondre"
    ),

    LOTTO4_BACK_PAIR(
        BetType.LOTTO4_PATTERN,
        (short) 4,
        "2 derniers chiffres",
        "Les 2 derniers chiffres du tirage doivent correspondre"
    ),

    // LOTO 5
    LOTTO5_LOT1_LOT2(
        BetType.LOTTO5_PATTERN,
        (short) 1,
        "1er lot + 2e lot",
        "3 chiffres du 1er lot suivis des 2 chiffres du 2e lot"
    ),

    LOTTO5_LOT1_LOT3(
        BetType.LOTTO5_PATTERN,
        (short) 2,
        "1er lot + 3e lot",
        "3 chiffres du 1er lot suivis des 2 chiffres du 3e lot"
    ),

    LOTTO5_MIXED_1_2_3(
        BetType.LOTTO5_PATTERN,
        (short) 3,
        "Mixte 1er/2e/3e lot",
        "Dernier chiffre du 1er lot + 2 chiffres du 2e lot + 2 chiffres du 3e lot"
    );

    private final BetType betType;
    private final short code;
    private final String label;
    private final String description;

    BetOption(BetType betType, short code, String label, String description) {
        this.betType = betType;
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public BetType betType() {
        return betType;
    }

    public short code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public static List<BetOption> allowedFor(BetType betType) {
        if (betType == null) {
            return List.of();
        }

        return Arrays.stream(values())
            .filter(option -> option.betType == betType)
            .toList();
    }

    public static boolean requiresOption(BetType betType) {
        return !allowedFor(betType).isEmpty();
    }

    public static BetOption from(BetType betType, Short code) {
        if (betType == null) {
            throw new IllegalArgumentException("betType is required");
        }

        var allowed = allowedFor(betType);

        if (allowed.isEmpty()) {
            if (code == null) {
                return null;
            }
            throw new IllegalArgumentException("BetType does not support options: " + betType);
        }

        if (code == null) {
            throw new IllegalArgumentException("betOption is required for betType: " + betType);
        }

        return allowed.stream()
            .filter(option -> option.code == code)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unsupported betOption " + code + " for betType " + betType
            ));
    }
}
