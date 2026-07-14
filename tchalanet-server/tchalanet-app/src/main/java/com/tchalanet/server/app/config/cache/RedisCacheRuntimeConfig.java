package com.tchalanet.server.app.config.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

@Configuration
@ConditionalOnProperty(name = "tch.cache.redis.enabled", havingValue = "true")
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
                RedisSerializationContext.SerializationPair.fromSerializer(cacheValueSerializer()));

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

  /**
   * Value serializer for the Redis (L2) cache. Package-visible + static so it can be exercised in a
   * round-trip test without a running Redis.
   *
   * <p>Design notes:
   *
   * <ul>
   *   <li>We deliberately do NOT register {@code TypedIdsJacksonModule}: its scalar serializer does
   *       not implement {@code serializeWithType()}, which the cache mapper's default typing
   *       (@class) requires. Typed-id records are handled by Jackson's default record serializer.
   *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES} is disabled so a record gaining a field does not break
   *       deserialization of entries cached by an older deploy (intermittent post-release errors).
   *   <li>{@code NullValue} is whitelisted because negative caching is enabled (we do not call
   *       {@code disableCachingNullValues}).
   * </ul>
   */
  static GenericJacksonJsonRedisSerializer cacheValueSerializer() {
    PolymorphicTypeValidator typeValidator =
        BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.tchalanet.server.")
            .allowIfSubType("java.lang.")
            .allowIfSubType("java.time.")
            .allowIfSubType("java.util.")
            .allowIfSubType("java.math.")
            .allowIfSubType("org.springframework.cache.interceptor.SimpleKey")
            .allowIfSubType("org.springframework.cache.support.NullValue")
            .allowIfSubType("tools.jackson.")
            .build();

    return GenericJacksonJsonRedisSerializer.builder()
        // Proper (de)serialization of Spring's NullValue (negative caching is enabled — we do
        // not call disableCachingNullValues).
        .enableSpringCacheNullValueSupport()
        // Force the root static type to Object so default typing also tags FINAL root values —
        // notably the immutable collections returned by Stream.toList()/List.of()/List.copyOf()
        // (java.util.ImmutableCollections$*), which NON_FINAL typing would otherwise leave
        // untyped and unreadable. The default writer uses the runtime type and misses these.
        .writer((mapper, source) -> mapper.writerFor(Object.class).writeValueAsBytes(source))
        .customize(
            builder ->
                builder
                    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                    // Resilience to schema drift: a record gaining a field must not break reads
                    // of entries cached by an older deploy.
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    // WRAPPER_ARRAY (not As.PROPERTY): wraps every value as [typeId, value],
                    // including root-level List/Map/scalar cache values. As.PROPERTY only tags
                    // objects and, under Jackson 3 (no As.PROPERTY→WRAPPER_ARRAY fallback), leaves
                    // root collections untyped — breaking the read of any List-valued cache
                    // (active_plans, active_games, resultslot:active, …).
                    .activateDefaultTyping(
                        typeValidator,
                        DefaultTyping.NON_FINAL_AND_RECORDS,
                        JsonTypeInfo.As.WRAPPER_ARRAY))
        .build();
  }
}
