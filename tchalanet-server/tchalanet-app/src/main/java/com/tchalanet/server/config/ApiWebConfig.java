package com.tchalanet.server.config;

import com.tchalanet.server.common.context.CurrentContextArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableConfigurationProperties({ApiProperties.class})
@RequiredArgsConstructor
public class ApiWebConfig implements WebMvcConfigurer {
    private final CurrentContextArgumentResolver resolver;
//    private final RequireIdempotencyInterceptor requireIdempotencyInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> rs) {
        rs.add(resolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // apply interceptor globally; it checks @RequireIdempotency presence per-handler
//todo get idempotency
//        registry.addInterceptor(requireIdempotencyInterceptor);
    }
}
