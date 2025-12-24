package com.tchalanet.server.common.web.advice;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for API response standardization.
 */
@Configuration
public class ApiResponseConfig {

    @Bean
    public FilterRegistrationBean<ApiResponseContextFilter> apiResponseContextFilter() {
        FilterRegistrationBean<ApiResponseContextFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ApiResponseContextFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1); // Run early
        return registrationBean;
    }
}
