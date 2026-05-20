package com.tchalanet.server.platform.identity.internal.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Simple in-memory cache mapping user keycloak sub -> Optional UUID with a short TTL. */
public final class UserSubToUuidCache {

  private static final Duration TTL = Duration.ofMinutes(5);

  private static final class Entry {
    final Optional<UUID> value;
    final Instant ts;

    Entry(Optional<UUID> value) {
      this.value = value;
      this.ts = Instant.now();
    }

    boolean fresh() {
      return Instant.now().isBefore(ts.plus(TTL));
    }
  }

  private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();

  public Optional<UUID> getFresh(String sub) {
    if (sub == null) return Optional.empty();
    Entry e = map.get(sub);
    if (e == null || !e.fresh()) return Optional.empty();
    return e.value;
  }

  public void put(String sub, Optional<UUID> value) {
    if (sub == null) return;
    map.put(sub, new Entry(value));
  }
}
