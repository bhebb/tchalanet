package com.tchalanet.server.common.config;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

/**
 * Profil local "insecure" pour simplifier le dev quand les certificats changent souvent. NE PAS
 * UTILISER EN PROD NI EN PREPROD.
 */
@Configuration
@Profile("insecure")
public class InsecureJwtDecoderConfig {

  @Bean
  JwtDecoder insecureJwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri)
      throws NoSuchAlgorithmException, KeyManagementException {

    // Trust manager permissif
    TrustManager[] trustAll =
        new TrustManager[] {
          new X509TrustManager() {
            // Intentionnel: profil 'insecure' ignore la validation pour accélérer le dev local.
            // Ne jamais activer hors environnement de développement.
            // Ces méthodes sont intentionnellement vides pour bypass SSL dans le profil 'insecure'.
            // Elles ne doivent jamais être modifiées pour de la logique métier ni utilisées en
            // prod.
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}

            public void checkServerTrusted(X509Certificate[] chain, String authType) {}

            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }
          }
        };

    SSLContext sc = SSLContext.getInstance("TLS");
    sc.init(null, trustAll, new SecureRandom());

    // HostnameVerifier permissif: usage limité au profil 'insecure'. Pour sécurité réelle,
    // laisser la vérification par défaut (ne pas activer ce profil).
    HostnameVerifier permissive = (h, s) -> true;
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    HttpsURLConnection.setDefaultHostnameVerifier(permissive);

    return JwtDecoders.fromIssuerLocation(issuerUri);
  }
}
