package com.tchalanet.server.platform.identity.internal.firebase;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@Profile("!insecure")
@ConditionalOnProperty(prefix = "tch.identity", name = "provider", havingValue = "firebase")
class FirebaseJwtDecoderConfig {

  @Bean
  JwtDecoder firebaseJwtDecoder(FirebaseIdentityProperties properties) {
    var projectId = properties.requiredProjectId();
    var issuer = properties.issuer();
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withJwkSetUri(properties.effectiveJwksUri()).build();

    OAuth2TokenValidator<Jwt> audienceValidator =
        jwt ->
            Optional.ofNullable(jwt.getAudience()).orElse(List.of()).contains(projectId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                        "invalid_token",
                        "Missing Firebase project audience: " + projectId,
                        null));

    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), audienceValidator));
    return decoder;
  }
}
