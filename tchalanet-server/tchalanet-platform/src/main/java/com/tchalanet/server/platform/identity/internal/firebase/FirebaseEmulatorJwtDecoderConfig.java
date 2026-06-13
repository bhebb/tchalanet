package com.tchalanet.server.platform.identity.internal.firebase;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Configuration
@ConditionalOnProperty(
    prefix = "tch.identity",
    name = "provider",
    havingValue = "firebase-emulator")
class FirebaseEmulatorJwtDecoderConfig {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  @Bean
  JwtDecoder firebaseEmulatorJwtDecoder(
      FirebaseIdentityProperties properties, ObjectMapper objectMapper) {
    var projectId = properties.requiredProjectId();
    OAuth2TokenValidator<Jwt> audienceValidator =
        jwt ->
            Optional.ofNullable(jwt.getAudience()).orElse(List.of()).contains(projectId)
                ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
                : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                        "invalid_token",
                        "Missing Firebase emulator project audience: " + projectId,
                        null));
    var validator =
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(properties.issuer()), audienceValidator);

    return token -> {
      try {
        var parts = token.split("\\.", -1);
        if (parts.length != 3 || !parts[2].isEmpty()) {
          throw new JwtException("Firebase emulator token must be unsigned");
        }
        Map<String, Object> headers = decode(parts[0], objectMapper);
        if (!"none".equals(headers.get("alg"))) {
          throw new JwtException("Firebase emulator token must use alg=none");
        }
        Map<String, Object> claims = decode(parts[1], objectMapper);
        var issuedAt = instantClaim(claims, "iat");
        var expiresAt = instantClaim(claims, "exp");
        var jwt = new Jwt(token, issuedAt, expiresAt, headers, claims);
        var result = validator.validate(jwt);
        if (result.hasErrors()) {
          throw new JwtException(result.getErrors().iterator().next().getDescription());
        }
        return jwt;
      } catch (JwtException ex) {
        throw ex;
      } catch (RuntimeException ex) {
        throw new JwtException("Invalid Firebase emulator token", ex);
      }
    };
  }

  private static Map<String, Object> decode(String value, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(Base64.getUrlDecoder().decode(value), MAP_TYPE);
    } catch (Exception ex) {
      throw new JwtException("Invalid Firebase emulator token encoding", ex);
    }
  }

  private static Instant instantClaim(Map<String, Object> claims, String name) {
    var value = claims.get(name);
    if (value instanceof Number number) {
      return Instant.ofEpochSecond(number.longValue());
    }
    throw new JwtException("Firebase emulator token claim '" + name + "' is required");
  }
}
