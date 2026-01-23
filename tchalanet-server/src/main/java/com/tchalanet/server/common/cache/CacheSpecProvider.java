package com.tchalanet.server.common.cache;

import java.util.List;

/**
 * Contrat pour déclarer les caches utilisés (cacheName + TTL L1/L2).
 */
public interface CacheSpecProvider {

    List<CacheSpec> cacheSpecs();
}
