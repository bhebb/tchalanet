package com.tchalanet.server.common.batch.key;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a validated batch job key.
 *
 * Format: <domain>:<capability>:<action> (minimum 3 segments)
 * Example: draw:lifecycle:settle, results:external:refresh
 *
 * Rules:
 * - segments separated by ':'
 * - only lowercase letters, digits, underscores allowed
 * - max length: 64 characters
 */
public final class JobKey {

    private static final int MAX_LENGTH = 64;
    private static final int MIN_SEGMENTS = 3;
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-z0-9_:]+$");
    private final String value;

    private JobKey(String value) {
        this.value = value;
    }

    /**
     * Create a JobKey from raw input.
     * Validates format and normalizes (trim, lowercase).
     *
     * @param raw the raw job key string
     * @return validated JobKey instance
     * @throws IllegalArgumentException if format is invalid
     */
    public static JobKey of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("jobKey cannot be null or blank");
        }

        String normalized = raw.trim().toLowerCase();

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "jobKey too long (max " + MAX_LENGTH + " chars): " + normalized);
        }

        if (!VALID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                "jobKey contains invalid characters (only [a-z0-9_:] allowed): " + normalized);
        }

        String[] segments = normalized.split(":");
        if (segments.length < MIN_SEGMENTS) {
            throw new IllegalArgumentException(
                "jobKey must have at least " + MIN_SEGMENTS + " segments (domain:capability:action): " + normalized);
        }

        return new JobKey(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobKey jobKey = (JobKey) o;
        return Objects.equals(value, jobKey.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
