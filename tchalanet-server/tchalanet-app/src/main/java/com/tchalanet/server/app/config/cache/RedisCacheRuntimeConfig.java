package com.tchalanet.server.app.config.cache;

import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ConditionalOnProperty(
    name = "tch.cache.redis.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class RedisCacheRuntimeConfig {

  private static final Logger log = LoggerFactory.getLogger(RedisCacheRuntimeConfig.class);

  @Value("${spring.data.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.data.redis.port:6379}")
  private int redisPort;

  @Value("${spring.data.redis.username:}")
  private String redisUsername;

  @Value("${spring.data.redis.password:}")
  private String redisPassword;

  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    log.info(
        "Creating LettuceConnectionFactory for Redis {}:{} (user-provided: {})",
        redisHost,
        redisPort,
        !redisPassword.isBlank());

    RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(redisHost, redisPort);

    if (!redisPassword.isBlank()) {
      cfg.setPassword(RedisPassword.of(redisPassword));
    }

    if (redisUsername != null && !redisUsername.isBlank()) {
      cfg.setUsername(redisUsername);
    }

    return new LettuceConnectionFactory(cfg);
  }

  @Bean
  public CacheManager redisCacheManager(
      LettuceConnectionFactory connectionFactory, List<CacheSpecProvider> specProviders) {
    RedisCacheConfiguration defaultCfg =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(60))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJacksonJsonRedisSerializer(JsonMapper.builder().build())));

    Map<String, RedisCacheConfiguration> perCacheCfg = new HashMap<>();
    if (specProviders != null) {
      specProviders.stream()
          .flatMap(p -> p.cacheSpecs().stream())
          .forEach(spec -> perCacheCfg.put(spec.name(), defaultCfg.entryTtl(spec.ttlL2())));
    }

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultCfg)
        .withInitialCacheConfigurations(perCacheCfg)
        .build();
  }

  @Bean
  public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
  }
}
