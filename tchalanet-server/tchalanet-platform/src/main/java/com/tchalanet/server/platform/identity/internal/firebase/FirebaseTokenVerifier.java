package com.tchalanet.server.platform.identity.internal.firebase;

import org.springframework.security.oauth2.jwt.Jwt;

public interface FirebaseTokenVerifier {

  Jwt verify(String bearerToken);
}
