package com.tchalanet.server.platform.idempotence.internal.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
class IdempotencyWebMvcConfig implements WebMvcConfigurer {

    private final RequireIdempotencyInterceptor requireIdempotencyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Applies globally; interceptor checks @RequireIdempotency per handler.
        registry.addInterceptor(requireIdempotencyInterceptor);
    }
}
