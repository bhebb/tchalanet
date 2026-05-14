package com.tchalanet.server.common.job.params;

import org.springframework.batch.core.job.parameters.JobParameters;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Type-safe reader for JobParameters.
 * <p>
 * Provides Optional-based and require-based accessors for common types.
 * Handles Spring Batch JobParameters format.
 */
public final class JobParamReader {

    private final JobParameters params;

    private JobParamReader(JobParameters params) {
        this.params = Objects.requireNonNull(params, "JobParameters");
    }

    /**
     * Create a reader from JobParameters.
     */
    public static JobParamReader of(JobParameters params) {
        return new JobParamReader(params);
    }

    /**
     * Get string parameter (optional).
     */
    public Optional<String> getString(String key) {
        String value = params.getString(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    /**
     * Get string parameter (required).
     *
     * @throws IllegalArgumentException if missing or blank
     */
    public String requireString(String key) {
        return getString(key).orElseThrow(() -> new IllegalArgumentException("Required parameter missing: " + key));
    }

    public Optional<Instant> getInstant(String key) {
        return getString(key).map(s -> {
            try {
                return Instant.ofEpochMilli(Long.parseLong(s));
            } catch (NumberFormatException ignored) {
                try {
                    return Instant.parse(s);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid instant parameter: " + key + "=" + s, e);
                }
            }
        });
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return getString(key).map(s -> {
            String normalized = s.toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "true", "1", "yes" -> true;
                case "false", "0", "no" -> false;
                default -> defaultValue;
            };
        }).orElse(defaultValue);
    }

    /**
     * Get Instant parameter (required).
     *
     * @throws IllegalArgumentException if missing or invalid
     */
    public Instant requireInstant(String key) {
        return getInstant(key).orElseThrow(() -> new IllegalArgumentException("Required parameter missing or invalid: " + key));
    }

    /**
     * Get Integer parameter (optional).
     */
    public Optional<Integer> getInt(String key) {
        return getString(key).map(s -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid integer parameter: " + key + "=" + s);
            }
        });
    }

    /**
     * Get Integer parameter (required).
     *
     * @throws IllegalArgumentException if missing or invalid
     */
    public int requireInt(String key) {
        return getInt(key).orElseThrow(() -> new IllegalArgumentException("Required parameter missing: " + key));
    }

    /**
     * Get Long parameter (optional).
     */
    public Optional<Long> getLong(String key) {
        return getString(key).map(s -> {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid long parameter: " + key + "=" + s);
            }
        });
    }

    /**
     * Get Long parameter (required).
     */
    public long requireLong(String key) {
        return getLong(key).orElseThrow(() -> new IllegalArgumentException("Required parameter missing: " + key));
    }
}
