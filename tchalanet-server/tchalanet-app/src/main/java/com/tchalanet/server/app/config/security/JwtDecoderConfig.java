package com.tchalanet.server.app.config.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@RequiredArgsConstructor
@Profile("!insecure")
public class JwtDecoderConfig {

    private final TchSecurityProperties securityProperties;

    @Bean
    @ConditionalOnProperty(
        prefix = "tch.identity",
        name = "provider",
        havingValue = "keycloak",
        matchIfMissing = true)
    JwtDecoder jwtDecoder(
        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {

        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);
        var requiredAudience = securityProperties.requiredAudience();

        OAuth2TokenValidator<Jwt> audienceValidator =
            jwt -> {
                List<String> aud = Optional.ofNullable(jwt.getAudience()).orElse(List.of());
                return aud.contains(requiredAudience)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                        "invalid_token",
                        "Missing required audience: " + requiredAudience,
                        null));
            };

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithIssuer(issuerUri));

        if (requiredAudience != null && !requiredAudience.isBlank()) {
            validators.add(audienceValidator);
        }

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

}
