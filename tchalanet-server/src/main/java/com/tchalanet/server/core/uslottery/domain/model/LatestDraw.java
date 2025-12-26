package com.tchalanet.server.core.uslottery.domain.model;

import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Value Object normalisé représentant un résultat externe.
 * Sert de transport interne entre provider clients -> adapter draw.
 */
public record LatestDraw(
    UsLotteryProvider provider,

    /** Identifiant du jeu côté provider (ex: "PICK3", "NUMBERS", "WIN4"). */
    String externalGameKey,

    /** Variante / type de tirage côté provider (ex: "MIDDAY", "EVENING"). */
    String externalDrawType,

    /** Channel interne Tchalanet (ex: "US_FL_NUM3_MID"). */
    String channelCode,

    /** Date locale du tirage (celle du provider). */
    LocalDate drawDate,

    /** Timestamp du tirage en UTC si disponible (sinon null). */
    OffsetDateTime occurredAtUtc,

    /** Timestamp du fetch (quand nous l’avons récupéré). */
    Instant fetchedAtUtc,

    /** Numéros normalisés (déjà nettoyés, dans l’ordre canonique). */
    DrawMain numbers,

    /** Données complémentaires (fireball, bonus, multiplier, etc.). */
    DrawExtras extras,

    /** Qualité du résultat (COMPLET / PARTIEL / SUSPECT). */
    ResultQuality quality,

    /** Origine technique (ex: "NY_OPEN_DATA", "FL_APIM"). */
    String origin,

    /** Référence optionnelle pour debug: url, query, trace id provider, etc. */
    Map<String, String> meta

) {
    public LatestDraw {
        Objects.requireNonNull(provider);
        requireNonBlank(externalGameKey, "externalGameKey");
        requireNonBlank(channelCode, "channelCode");
        Objects.requireNonNull(drawDate, "drawDate");
        Objects.requireNonNull(fetchedAtUtc, "fetchedAtUtc");
        Objects.requireNonNull(numbers, "numbers");
        Objects.requireNonNull(quality, "quality");
        requireNonBlank(origin, "origin");
        meta = meta == null ? Map.of() : Map.copyOf(meta);
    }

    /**
     * Clé d’idempotence stable pour “déjà traité”.
     */
    public String idempotencyKey() {
        var type = (externalDrawType == null || externalDrawType.isBlank())
            ? "UNKNOWN"
            : externalDrawType.trim().toUpperCase();

        return provider.name() + ":" + channelCode + ":" + drawDate + ":" + type;
    }

    private static void requireNonBlank(String s, String name) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }
}
