package com.tchalanet.server.core.draw.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * DrawResult (canonique, global)
 *
 * <p>Vérité canonique des résultats par (channel_code, draw_date). Sert de source unique pour tous
 * les tenants.
 *
 * <p>Champs clés: - channel_code, draw_date (unicité) - numbers_main, numbers_extra, occurred_at -
 * quality (SUSPECT, COMPLETE), status (VALID, OVERRIDDEN, etc.) - source, source_hash, raw_payload,
 * override_reason, fetched_at
 *
 * <p>Invariants: - Écrire COMPLETE seulement si le résultat est fiable (ou force=true lors d'une
 * override). - Ne jamais dégrader COMPLETE en SUSPECT.
 */
public record DrawResult(
    DrawSource source,
    List<String> numbersMain,
    List<String> numbersExtra,
    Instant occurredAt,
    String rawPayload, // pour audit, peut être null
    boolean overridden,
    String overrideReason) {
  public DrawResult {
    Objects.requireNonNull(source);
    Objects.requireNonNull(numbersMain);
    Objects.requireNonNull(numbersExtra);
    Objects.requireNonNull(occurredAt);
    Objects.requireNonNull(rawPayload);
  }

  public DrawResult override(
      List<String> newNumbersMain, List<String> newNumbersExtra, String reason) {
    return new DrawResult(
        DrawSource.ADMIN_OVERRIDE,
        List.copyOf(newNumbersMain),
        newNumbersExtra != null ? List.copyOf(newNumbersExtra) : null,
        Instant.now(),
        this.rawPayload,
        true,
        reason);
  }
}
