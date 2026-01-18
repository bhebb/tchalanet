package com.tchalanet.server.catalog.pricing.internal.rest;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.context.annotation.Bean;

@Configuration
class PricingOddsRestSecurityConfig {

  @Bean
  SecurityFilterChain pricingOddsRestChain(HttpSecurity http) throws Exception {
    http
      .securityMatcher("/platform/pricing-odds/**", "/platform/pricing-odds") // adapte le base-path Data REST
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.GET, "/platform/pricing-odds/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
        .requestMatchers(HttpMethod.POST, "/platform/pricing-odds/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
        .requestMatchers(HttpMethod.PUT, "/platform/pricing-odds/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
        .requestMatchers(HttpMethod.PATCH, "/platform/pricing-odds/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
        .requestMatchers(HttpMethod.DELETE, "/platform/pricing-odds/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
        .anyRequest().denyAll()
      );
    return http.build();
  }
}

