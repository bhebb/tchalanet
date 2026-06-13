package com.tchalanet.server.platform.identity.internal.firebase;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression(
    "'${tch.identity.provider:firebase}' == 'firebase' || '${tch.identity.provider:firebase}' == 'firebase-emulator'")
final class FirebaseJwtTokenVerifier implements FirebaseTokenVerifier {

  private final JwtDecoder jwtDecoder;

  @Override
  public Jwt verify(String bearerToken) {
    return jwtDecoder.decode(bearerToken);
  }
}
