package com.tchalanet.server.common.web.observability;

import com.tchalanet.server.common.observability.TchObservabilityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Registers observability filters as servlet filters that run before Spring Security.
 *
 * <p>Order layout:
 * <ul>
 *   <li>{@code HIGHEST_PRECEDENCE + 5}  — RequiredRequestIdFilter (validate + MDC)</li>
 *   <li>{@code HIGHEST_PRECEDENCE + 7}  — ServletExceptionBridgeFilter (filter exceptions)</li>
 *   <li>{@code HIGHEST_PRECEDENCE + 10} — TraceResponseHeaderFilter (response headers)</li>
 *   <li>Spring Security at -100</li>
 *   <li>BearerTokenAuthenticationFilter, IdentityBootstrapFilter, TchContextFilter inside Security</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(TchObservabilityProperties.class)
public class ObservabilityWebConfig {

    @Bean
    FilterRegistrationBean<RequiredRequestIdFilter> requiredRequestIdFilterRegistration(
        TchObservabilityProperties properties
    ) {
        var filter = new RequiredRequestIdFilter(properties);
        var bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        bean.addUrlPatterns("/*");
        bean.setName("requiredRequestIdFilter");
        return bean;
    }

    @Bean
    FilterRegistrationBean<ServletExceptionBridgeFilter> servletExceptionBridgeFilterRegistration(
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
    ) {
        var filter = new ServletExceptionBridgeFilter(exceptionResolver);
        var bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 7);
        bean.addUrlPatterns("/*");
        bean.setName("servletExceptionBridgeFilter");
        return bean;
    }

    @Bean
    FilterRegistrationBean<TraceResponseHeaderFilter> traceResponseHeaderFilterRegistration(
        TchObservabilityProperties properties
    ) {
        var filter = new TraceResponseHeaderFilter(properties);
        var bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        bean.addUrlPatterns("/*");
        bean.setName("traceResponseHeaderFilter");
        return bean;
    }
}
