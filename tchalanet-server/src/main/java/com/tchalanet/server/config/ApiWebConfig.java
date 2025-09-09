package com.tchalanet.server.config;

import com.tchalanet.server.config.context.CurrentContextArgumentResolver;
import com.tchalanet.server.config.properties.ApiProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(ApiProperties.class)
@RequiredArgsConstructor
public class ApiWebConfig implements WebMvcConfigurer {
  private final ApiProperties apiProperties;
  private final CurrentContextArgumentResolver resolver;

  @Override
  public void configurePathMatch(
      org.springframework.web.servlet.config.annotation.PathMatchConfigurer configurer) {
    //    configurer.setUseSuffixPatternMatch(false);
    var base = this.apiProperties.basePath().replaceAll("/+$", "");
    var version = this.apiProperties.apiVersion().replaceAll("/+$", "");
    var prefix = base + "/" + version;
    configurer.addPathPrefix(prefix, HandlerTypePredicate.forAnnotation(RestController.class));
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> rs) {
    rs.add(resolver);
  }
}
