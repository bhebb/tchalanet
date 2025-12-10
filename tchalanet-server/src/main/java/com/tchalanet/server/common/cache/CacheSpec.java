package com.tchalanet.server.common.cache;

import java.time.Duration;

/**
 * Spécification d'un cache de domaine: nom logique du cache Spring et TTL L2 (Redis).
 */
public record CacheSpec(String name, Duration ttlL2) {}
