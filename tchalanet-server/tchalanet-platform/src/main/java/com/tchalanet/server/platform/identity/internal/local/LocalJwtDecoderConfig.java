package com.tchalanet.server.platform.identity.internal.local;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

@Configuration
@ConditionalOnExpression(
    "'${tch.identity.provider:keycloak}' == 'local-jwt' || '${tch.identity.provider:keycloak}' == 'local-perf'")
class LocalJwtDecoderConfig {

  @Bean
  JwtDecoder localJwtDecoder(LocalIdentityProperties properties) {
    var secretKey =
        new SecretKeySpec(properties.requiredSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    var decoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(), new JwtIssuerValidator(properties.requiredIssuer())));
    return decoder;
  }
}
