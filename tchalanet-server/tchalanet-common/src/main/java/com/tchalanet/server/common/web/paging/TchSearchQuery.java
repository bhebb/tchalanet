package com.tchalanet.server.common.web.paging;

/**
 * Normalized free-text search input for admin list queries.
 * Each feature decides which fields are searched against this value.
 */
public record TchSearchQuery(String value) {

    public static TchSearchQuery of(String raw) {
        if (raw == null || raw.isBlank()) return empty();
        return new TchSearchQuery(raw.trim());
    }

    public static TchSearchQuery empty() {
        return new TchSearchQuery(null);
    }

    public boolean isPresent() {
        return value != null && !value.isBlank();
    }

    /** LIKE pattern for case-insensitive SQL matching: {@code %value%}. Returns null when empty. */
    public String likePattern() {
        return isPresent() ? "%" + value.toLowerCase() + "%" : null;
    }
}
