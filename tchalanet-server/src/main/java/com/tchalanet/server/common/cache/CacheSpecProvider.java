package com.tchalanet.server.common.cache;

import java.util.List;

/** Contrat pour qu'un module métier déclare les caches qu'il utilise (nom + TTL L2). */
public interface CacheSpecProvider {

  List<CacheSpec> cacheSpecs();
}
