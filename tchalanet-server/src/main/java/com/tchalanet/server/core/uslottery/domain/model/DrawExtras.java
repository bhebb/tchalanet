package com.tchalanet.server.core.uslottery.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Extras éventuels (bonus ball, fireball, etc.).
 */
public record DrawExtras(
    List<Integer> extraNumbers,
    Map<String, String> attributes
) {
    public DrawExtras {
        extraNumbers = extraNumbers == null ? List.of() : List.copyOf(extraNumbers);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static DrawExtras empty() {
        return new DrawExtras(List.of(), Map.of());
    }
}
