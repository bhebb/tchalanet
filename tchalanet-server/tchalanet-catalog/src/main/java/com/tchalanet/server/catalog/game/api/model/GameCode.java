package com.tchalanet.server.catalog.game.api.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Codes produits vendus.
 * Doivent correspondre aux seeds de la table game (code unique).
 * <p>
 * IMPORTANT:
 * - Provider-agnostic (HT_* uniquement)
 * - Stable dans le temps
 */
public enum GameCode {

    HT_BOLET(Set.of(BetType.MATCH_1_2D, BetType.MATCH_2_2D, BetType.MATCH_3_2D)),

    HT_MARYAJ(Set.of(BetType.MARRIAGE_2D2D)),

    HT_MARYAJ_GRATUIT(Set.of(BetType.MARRIAGE_2D2D)),

    HT_LOTO3(Set.of(BetType.LOTTO3_3D)),

    HT_LOTO4(Set.of(BetType.LOTTO4_PATTERN)),

    HT_LOTO5(Set.of(BetType.LOTTO5_PATTERN));

    private final Set<BetType> allowedBetTypes;

    GameCode(Set<BetType> allowedBetTypes) {
        this.allowedBetTypes = allowedBetTypes;
    }

    public boolean supports(BetType betType) {
        return betType != null && allowedBetTypes.contains(betType);
    }

    public Set<BetType> allowedBetTypes() {
        return EnumSet.copyOf(allowedBetTypes);
    }
}
