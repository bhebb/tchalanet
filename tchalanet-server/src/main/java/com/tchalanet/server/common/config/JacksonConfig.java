package com.tchalanet.server.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tchalanet.server.common.web.jackson.TypedIdsJacksonModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;

@Configuration
public class JacksonConfig {

  @Bean
  public Module typedIdsModule() {
    return TypedIdsJacksonModule.create();
  }

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
    return builder -> {
      builder.modules(typedIdsModule());
      builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
      builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    };
  }
}
