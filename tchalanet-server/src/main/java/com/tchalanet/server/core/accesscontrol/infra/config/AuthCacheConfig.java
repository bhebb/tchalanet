package com.tchalanet.server.core.accesscontrol.infra.config;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthCacheConfig {

  @Bean
  public CacheSpecProvider authCacheSpecProvider() {
    return () ->
        List.of(
            // Permissions utilisateur : 5–15 min, v1 = 10 min
            new CacheSpec("user_permissions", Duration.ofMinutes(10)),
            // Profil utilisateur de base : 10–30 min, v1 = 20 min
            new CacheSpec("user_profile", Duration.ofMinutes(20)),
            // Matrice rôles → permissions : 30–60 min, v1 = 45 min
            new CacheSpec("tenant_roles_matrix", Duration.ofMinutes(45))
        );
  }
}

