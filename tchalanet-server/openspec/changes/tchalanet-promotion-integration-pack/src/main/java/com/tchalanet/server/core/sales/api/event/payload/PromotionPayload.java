package com.tchalanet.server.core.sales.api.event.payload;

import java.time.Instant;
import java.util.List;

/** Lightweight event snapshot. Not an input to promotion command. */
public record PromotionPayload(
    String decisionId,
    String status,
    String phase,
    Instant evaluatedAt,
    String contextHash,
    String engineVersion,
    List<PromotionEffectPayload> effects,
    List<String> notices
) {
    public PromotionPayload {
        effects = effects == null ? List.of() : List.copyOf(effects);
        notices = notices == null ? List.of() : List.copyOf(notices);
    }
}
