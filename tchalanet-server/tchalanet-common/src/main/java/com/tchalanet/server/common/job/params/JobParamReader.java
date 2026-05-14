package com.tchalanet.server.common.job.params;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure-Java type-safe reader for job parameters.
 *
 * <p>Operates on {@code Map<String,String>}. Adapters that read Spring Batch {@code JobParameters}
 * must convert to a string map before passing in.
 */
public final class JobParamReader {

  private final Map<String, String> params;

  private JobParamReader(Map<String, String> params) {
    this.params = Objects.requireNonNull(params, "params");
  }

  public static JobParamReader of(Map<String, String> params) {
    return new JobParamReader(params);
  }

  public Optional<String> getString(String key) {
    String value = params.get(key);
    if (value == null || value.isBlank()) return Optional.empty();
    return Optional.of(value.trim());
  }

  public String requireString(String key) {
    return getString(key)
        .orElseThrow(() -> new IllegalArgumentException("Required parameter missing: " + key));
  }

  public Optional<Instant> getInstant(String key) {
    return getString(key)
        .map(
            s -> {
              try {
                return Instant.ofEpochMilli(Long.parseLong(s));
              } catch (NumberFormatException ignored) {
                try {
                  return Instant.parse(s);
                } catch (Exception e) {
                  throw new IllegalArgumentException(
                      "Invalid instant parameter: " + key + "=" + s, e);
                }
              }
            });
  }

  public Instant requireInstant(String key) {
    return getInstant(key)
        .orElseThrow(
            () -> new IllegalArgumentException("Required parameter missing or invalid: " + key));
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    return getString(key)
        .map(
            s -> {
              String normalized = s.toLowerCase(Locale.ROOT);
              return switch (normalized) {
                case "true", "1", "yes" -> true;
                case "false", "0", "no" -> false;
                default -> defaultValue;
              };
            })
        .orElse(defaultValue);
  }

  public Optional<Integer> getInt(String key) {
    return getString(key)
        .map(
            s -> {
              try {
                return Integer.parseInt(s);
              } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid integer parameter: " + key + "=" + s);
              }
            });
  }

  public int requireInt(String key) {
    return getInt(key)
        .orElseThrow(() -> new IllegalArgumentException("Required parameter missing: " + key));
  }

  public Optional<Long> getLong(String key) {
    return getString(key)
        .map(
            s -> {
              try {
                return Long.parseLong(s);
              } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid long parameter: " + key + "=" + s);
              }
            });
  }

  public long requireLong(String key) {
    return getLong(key)
        .orElseThrow(() -> new IllegalArgumentException("Required parameter missing: " + key));
  }
}
