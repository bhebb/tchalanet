package com.tchalanet.server.common.security;

import com.tchalanet.server.common.context.TenantContextInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache mapping tenantCode -> TenantContextInfo (short TTL).
 *
 * Used during bootstrap/authentication flows to avoid repeated database lookups.
 * Not a replacement for proper cache infrastructure (Caffeine/Redis).
 *
 * Thread-safe via ConcurrentHashMap.
 * Entries expire after TTL (default: 5 minutes).
 */
public final class TenantCodeToContextInfoCache {

  private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

  private static final class Entry {
    final TenantContextInfo value; // nullable -> null means "not found" cached
    final Instant storedAt;

    Entry(TenantContextInfo value, Instant storedAt) {
      this.value = value;
      this.storedAt = storedAt;
    }
  }

  private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();
  private final Duration ttl;
  private final Clock clock;

  public TenantCodeToContextInfoCache(Clock clock) {
    this(DEFAULT_TTL, clock);
  }

  public TenantCodeToContextInfoCache(Duration ttl, Clock clock) {
    this.ttl = ttl == null ? DEFAULT_TTL : ttl;
    this.clock = clock;
  }

  /**
   * Get fresh (non-expired) tenant info from cache.
   *
   * @param tenantCode tenant code
   * @return tenant info if present and fresh, empty otherwise
   */
  public Optional<TenantContextInfo> getFresh(String tenantCode) {
    if (tenantCode == null) {
      return Optional.empty();
    }

    Entry e = map.get(tenantCode);
    if (e == null) {
      return Optional.empty();
    }

    Instant now = clock.instant();
    if (now.isAfter(e.storedAt.plus(ttl))) {
      return Optional.empty();
    }

    return Optional.ofNullable(e.value);
  }

  /**
   * Put tenant info in cache.
   *
   * @param tenantCode tenant code
   * @param value tenant context info (nullable). Passing null caches a negative result (not found).
   */
  public void put(String tenantCode, TenantContextInfo value) {
    if (tenantCode == null) {
      return;
    }
    map.put(tenantCode, new Entry(value, clock.instant()));
  }
}
