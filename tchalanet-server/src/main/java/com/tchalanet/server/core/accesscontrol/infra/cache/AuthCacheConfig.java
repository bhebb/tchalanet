package com.tchalanet.server.core.accesscontrol.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AuthCacheConfig implements CacheSpecProvider {

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        // Profil utilisateur de base : 10–30 min, v1 = 20 min
        CacheSpec.of("user_profile", Duration.ofMinutes(20)),
        // Matrice rôles → permissions (hiérarchie) : 30–60 min, v1 = 45 min
        CacheSpec.of("role-permissions", Duration.ofMinutes(45)));
  }
}
