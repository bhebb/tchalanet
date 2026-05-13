package com.tchalanet.server.common.json;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 3 configuration.
 *
 * <p>Spring Boot 4 ships Jackson 3 by default via {@code spring-boot-starter-json}. The framework
 * auto-discovers modules through the JDK {@link java.util.ServiceLoader} mechanism and applies any
 * {@link JsonMapperBuilderCustomizer} bean to the immutable {@link JsonMapper} built at startup.
 *
 * <p>This config:
 *
 * <ul>
 *   <li>Registers the project-specific {@link TypedIdsJacksonModule} (typed-id wrappers).
 *   <li>Tunes serialization defaults (ISO-8601 dates, lenient deserialization).
 *   <li>Exposes a singleton {@link JsonMapper} bean for non-Spring-managed callers (JPA
 *       AttributeConverters, utility classes).
 * </ul>
 */
@Configuration
public class JacksonConfig {

    /**
     * Project-specific module registering serializers/deserializers for typed-id wrappers.
     */
    @Bean
    public JacksonModule typedIdsModule() {
        return TypedIdsJacksonModule.create();
    }

    /**
     * Customizes the auto-configured {@link JsonMapper} produced by Spring Boot. Registered modules
     * (including {@link #typedIdsModule()}) are applied here.
     */
    @Bean
    public JsonMapperBuilderCustomizer jacksonCustomizer() {
        return builder ->
            builder
                .addModule(typedIdsModule())
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
