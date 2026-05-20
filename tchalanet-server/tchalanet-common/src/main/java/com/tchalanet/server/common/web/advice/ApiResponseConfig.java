package com.tchalanet.server.common.web.advice;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ApiResponseConfig {

    @Bean
    public FilterRegistrationBean<ApiResponseContextFilter> apiResponseContextFilter() {
        var registration = new FilterRegistrationBean<ApiResponseContextFilter>();
        registration.setName("apiResponseContextFilter");
        registration.setFilter(new ApiResponseContextFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
