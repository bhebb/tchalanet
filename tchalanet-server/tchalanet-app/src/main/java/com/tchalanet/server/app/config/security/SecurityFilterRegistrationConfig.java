package com.tchalanet.server.app.config.security;

import com.tchalanet.server.common.context.web.TchContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prevents double servlet-container registration for filters that are both Spring beans and
 * manually added to the Spring Security chain in {@link SecurityConfig}.
 *
 * <p>Rule: any filter that is a Spring bean AND added via {@code addFilterAfter/Before} must have a
 * disabled {@link FilterRegistrationBean}, otherwise Spring Boot auto-registers it as a servlet
 * filter and it fires twice (once outside the security chain, once inside).
 *
 * <p>{@code SensitiveIdentityVerificationFilter} is intentionally absent: it is instantiated with
 * {@code new} in {@link SecurityConfig} and is not a Spring bean, so it is never auto-registered.
 */
@Configuration
public class SecurityFilterRegistrationConfig {

    @Bean
    FilterRegistrationBean<TchContextFilter> tchContextFilterRegistration(TchContextFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<TchAccessContextPipelineFilter> tchAccessContextPipelineFilterRegistration(
        TchAccessContextPipelineFilter filter
    ) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
