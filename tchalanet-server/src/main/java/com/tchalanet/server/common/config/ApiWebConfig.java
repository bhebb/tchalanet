package com.tchalanet.server.common.config;

import com.tchalanet.server.common.context.CurrentContextArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(ApiProperties.class)
@RequiredArgsConstructor
public class ApiWebConfig implements WebMvcConfigurer {
  private final ApiProperties apiProperties;
  private final CurrentContextArgumentResolver resolver;

  // removed configurePathMatch: servlet path will provide the API prefix (/api/v1)

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> rs) {
    rs.add(resolver);
  }
}
