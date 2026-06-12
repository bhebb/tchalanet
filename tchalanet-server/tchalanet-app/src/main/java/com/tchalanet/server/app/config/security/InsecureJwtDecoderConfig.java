package com.tchalanet.server.app.config.security;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.util.List;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Profil local "insecure" pour simplifier le dev quand les certificats changent souvent. NE PAS
 * UTILISER EN PROD NI EN PREPROD.
 */
@Configuration
@Profile("insecure")
@ConditionalOnProperty(
    prefix = "tch.identity",
    name = "provider",
    havingValue = "keycloak",
    matchIfMissing = true)
public class InsecureJwtDecoderConfig {

  @Bean
  JwtDecoder insecureJwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri)
      throws NoSuchAlgorithmException, KeyManagementException {

    // Trust manager permissif pour les connexions HTTPS sortantes du serveur (ex: introspection).
    // Ne jamais activer hors environnement de développement.
    TrustManager[] trustAll =
        new TrustManager[] {
          new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}

            public void checkServerTrusted(X509Certificate[] chain, String authType) {}

            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }
          }
        };

    SSLContext sc = SSLContext.getInstance("TLS");
    sc.init(null, trustAll, new SecureRandom());

    HostnameVerifier permissive = (h, s) -> true;
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    HttpsURLConnection.setDefaultHostnameVerifier(permissive);

    // Build decoder directly from JWK URI — évite la discovery OIDC au démarrage.
    // La discovery appelle issuer-uri depuis Docker où localtest.me → 127.0.0.1 (le container).
    //
    // IMPORTANT: ne pas utiliser JwtValidators.createDefaultWithIssuer() — en Spring Security 6.4+
    // cette méthode déclenche une OIDC discovery réseau, ce qui échoue depuis Docker.
    // JwtIssuerValidator fait une simple comparaison de string sur le claim "iss", sans réseau.
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    List<OAuth2TokenValidator<Jwt>> validators = List.of(
        new JwtTimestampValidator(),
        new JwtIssuerValidator(issuerUri)
    );
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
    return decoder;
  }
}
