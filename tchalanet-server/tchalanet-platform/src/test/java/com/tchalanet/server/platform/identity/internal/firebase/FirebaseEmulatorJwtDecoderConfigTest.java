package com.tchalanet.server.platform.identity.internal.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import tools.jackson.databind.ObjectMapper;

class FirebaseEmulatorJwtDecoderConfigTest {

    private static final String PROJECT_ID = "demo-tchalanet-local";

    private final FirebaseIdentityProperties properties =
        new FirebaseIdentityProperties(PROJECT_ID, null, null, FirebaseRevocationCheckMode.OFF, "@test.test");
    private final FirebaseEmulatorJwtDecoderConfig config = new FirebaseEmulatorJwtDecoderConfig();

    @Test
    void acceptsUnsignedTokenForExpectedEmulatorProject() {
        var jwt = config.firebaseEmulatorJwtDecoder(properties, new ObjectMapper()).decode(token(""));

        assertThat(jwt.getSubject()).isEqualTo("firebase-emulator-user");
        assertThat(jwt.getAudience()).containsExactly(PROJECT_ID);
    }

    @Test
    void rejectsTokenWithSignature() {
        var decoder = config.firebaseEmulatorJwtDecoder(properties, new ObjectMapper());

        assertThatThrownBy(() -> decoder.decode(token("unexpected-signature")))
            .isInstanceOf(JwtException.class)
            .hasMessageContaining("unsigned");
    }

    private static String token(String signature) {
        var now = Instant.now();
        var header = encode("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        var claims =
            encode(
                """
                    {"sub":"firebase-emulator-user","aud":"%s","iss":"https://securetoken.google.com/%s","iat":%d,"exp":%d}
                    """
                    .formatted(PROJECT_ID, PROJECT_ID, now.getEpochSecond(), now.plusSeconds(300).getEpochSecond()));
        return header + "." + claims + "." + signature;
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
