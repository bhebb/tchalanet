package com.tchalanet.server.platform.identity.internal.firebase;

import com.tchalanet.server.platform.identity.api.IdentityProviderException;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.IdentityVerificationPolicy;
import com.tchalanet.server.platform.identity.api.VerifiedExternalToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FirebaseIdentityProviderTest {

    private static final String PROJECT_ID = "tchalanet-test";
    private static final String ISSUER = "https://securetoken.google.com/" + PROJECT_ID;

    private final FirebaseTokenVerifier verifier = Mockito.mock(FirebaseTokenVerifier.class);
    private final FirebaseRevocationChecker revocationChecker =
        Mockito.mock(FirebaseRevocationChecker.class);
    private final FirebaseIdentityProvider provider =
        provider(FirebaseRevocationCheckMode.SENSITIVE_ONLY);

    @Test
    void verifiesFirebaseTokenAndExposesOnlySafeIdentityClaims() {
        when(verifier.verify("token"))
            .thenReturn(
                jwt(
                    Map.of(
                        "iss", ISSUER,
                        "aud", PROJECT_ID,
                        "sub", "firebase-uid",
                        "email", "cashier@example.com",
                        "email_verified", true,
                        "phone_number", "+15550000000",
                        "firebase", Map.of("sign_in_provider", "phone"),
                        "roles", java.util.List.of("SUPER_ADMIN"))));

        var user = provider.verifyBearerToken("token", IdentityVerificationPolicy.STANDARD);

        assertThat(user.provider()).isEqualTo(IdentityProviderType.FIREBASE);
        assertThat(user.issuer()).isEqualTo(ISSUER);
        assertThat(user.subject()).isEqualTo("firebase-uid");
        assertThat(user.safeClaims()).containsOnlyKeys("phone_number", "firebase");
    }

    @Test
    void rejectsWrongIssuer() {
        assertInvalidClaim(
            new VerifiedExternalToken(
                "https://securetoken.google.com/another-project",
                "firebase-uid",
                null,
                false,
                Map.of("aud", PROJECT_ID)),
            "invalid_issuer");
    }

    @Test
    void rejectsWrongAudience() {
        assertInvalidClaim(
            new VerifiedExternalToken(
                ISSUER, "firebase-uid", null, false, Map.of("aud", "another-project")),
            "invalid_audience");
    }

    @Test
    void rejectsMissingSubject() {
        when(verifier.verify("token"))
            .thenReturn(jwt(Map.of("iss", ISSUER, "aud", PROJECT_ID)));

        assertThatThrownBy(
            () -> provider.verifyBearerToken("token", IdentityVerificationPolicy.STANDARD))
            .isInstanceOf(IdentityProviderException.class)
            .extracting("code")
            .isEqualTo("invalid_token");
    }

    @Test
    void rejectsSubjectLongerThanFirebaseLimit() {
        assertInvalidClaim(
            new VerifiedExternalToken(
                ISSUER, "x".repeat(129), null, false, Map.of("aud", PROJECT_ID)),
            "invalid_subject");
    }

    @ParameterizedTest(name = "rejects decoder failure: {0}")
    @ValueSource(strings = {"malformed token", "expired token", "signature verification failed"})
    void rejectsDecoderFailures(String reason) {
        when(verifier.verify("token")).thenThrow(new JwtException(reason));

        assertThatThrownBy(
            () -> provider.verifyBearerToken("token", IdentityVerificationPolicy.STANDARD))
            .isInstanceOf(IdentityProviderException.class)
            .extracting("code")
            .isEqualTo("invalid_token");
    }

    @ParameterizedTest(name = "{0} with {1} requires revocation check: {2}")
    @CsvSource({
        "OFF, STANDARD, false",
        "OFF, SENSITIVE, false",
        "SENSITIVE_ONLY, STANDARD, false",
        "SENSITIVE_ONLY, SENSITIVE, true",
        "ALWAYS, STANDARD, true",
        "ALWAYS, SENSITIVE, true"
    })
    void appliesConfiguredRevocationPolicy(
        FirebaseRevocationCheckMode mode,
        IdentityVerificationPolicy policy,
        boolean expectedCheck) {
        var token =
            new VerifiedExternalToken(
                ISSUER,
                "firebase-uid",
                null,
                false,
                Map.of("aud", PROJECT_ID, "auth_time", 1_750_000_000L));

        provider(mode).mapVerifiedToken(token, policy);

        if (expectedCheck) {
            verify(revocationChecker).check(token);
        } else {
            verifyNoInteractions(revocationChecker);
        }
    }

    private void assertInvalidClaim(VerifiedExternalToken token, String code) {
        assertThatThrownBy(() -> provider.mapVerifiedToken(token, IdentityVerificationPolicy.STANDARD))
            .isInstanceOf(IdentityProviderException.class)
            .extracting("code")
            .isEqualTo(code);
    }

    private FirebaseIdentityProvider provider(FirebaseRevocationCheckMode mode) {
        return new FirebaseIdentityProvider(
            verifier, new FirebaseIdentityProperties(PROJECT_ID, null, null, mode, "test.com"), revocationChecker);
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
            "token",
            Instant.parse("2026-06-12T10:00:00Z"),
            Instant.parse("2026-06-12T11:00:00Z"),
            Map.of("alg", "RS256"),
            claims);
    }
}
