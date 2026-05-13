package com.tchalanet.server.app.config.security;

import com.tchalanet.server.common.context.web.TchContextFilter;
import com.tchalanet.server.platform.identity.internal.service.UserBootstrapFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@RequiredArgsConstructor
@Profile("!insecure")
public class SecurityFilterRegistrationConfig {
    @Bean
    FilterRegistrationBean<UserBootstrapFilter> userBootstrapFilterRegistration(
        UserBootstrapFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<TchContextFilter> tchContextFilterRegistration(TchContextFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
