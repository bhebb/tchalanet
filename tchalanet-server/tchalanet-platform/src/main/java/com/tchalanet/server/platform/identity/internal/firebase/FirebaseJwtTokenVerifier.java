package com.tchalanet.server.platform.identity.internal.firebase;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tch.identity", name = "provider", havingValue = "firebase")
final class FirebaseJwtTokenVerifier implements FirebaseTokenVerifier {

  private final JwtDecoder jwtDecoder;

  @Override
  public Jwt verify(String bearerToken) {
    return jwtDecoder.decode(bearerToken);
  }
}
