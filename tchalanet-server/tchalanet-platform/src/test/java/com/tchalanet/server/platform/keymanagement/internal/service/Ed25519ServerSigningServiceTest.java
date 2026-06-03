package com.tchalanet.server.platform.keymanagement.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.crypto.Ed25519SignatureVerifier;
import com.tchalanet.server.platform.keymanagement.api.model.ServerSigningPurpose;
import com.tchalanet.server.platform.keymanagement.internal.config.KeyManagementProperties;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class Ed25519ServerSigningServiceTest {

    private static final Ed25519SignatureVerifier VERIFIER = new Ed25519SignatureVerifier();

    @Mock
    private Environment environment;

    // ── Ephemeral (dev) mode ──────────────────────────────────────────────────

    @Test
    void ephemeralModeSignsAndVerifiesRoundTrip() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local-ide"});
        var service = buildService(null, null);

        var payload = "OFFLINE_GRANT\ntest-payload".getBytes(StandardCharsets.UTF_8);
        var result = service.sign(ServerSigningPurpose.OFFLINE_GRANT, payload);

        assertThat(result.signature()).isNotBlank();
        assertThat(result.algorithm()).isEqualTo("Ed25519");
        assertThat(result.keyId()).isEqualTo("test-key-id");

        var publicKeySet = service.listActivePublicKeys();
        assertThat(publicKeySet.activeKeyId()).isEqualTo("test-key-id");
        assertThat(publicKeySet.keys()).hasSize(1);

        var spki = Base64.getDecoder().decode(publicKeySet.keys().get(0).publicKey());
        var sig  = Base64.getUrlDecoder().decode(result.signature());
        assertThat(VERIFIER.verify(spki, payload, sig)).isTrue();
    }

    @Test
    void ephemeralModeGrantsDifferentSignaturesOnDifferentPayloads() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        var service = buildService(null, null);

        var r1 = service.sign(ServerSigningPurpose.OFFLINE_GRANT, "payload-a".getBytes(StandardCharsets.UTF_8));
        var r2 = service.sign(ServerSigningPurpose.OFFLINE_GRANT, "payload-b".getBytes(StandardCharsets.UTF_8));

        assertThat(r1.signature()).isNotEqualTo(r2.signature());
    }

    // ── Configured (prod-like) mode ───────────────────────────────────────────

    @Test
    void configuredModeSignsAndVerifiesRoundTripWithRealKeyPair() throws Exception {
        var kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        var privB64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        var pubB64  = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

        var service = buildService(privB64, pubB64);

        var payload = "OFFLINE_GRANT\nconfigured-test".getBytes(StandardCharsets.UTF_8);
        var result = service.sign(ServerSigningPurpose.OFFLINE_GRANT, payload);

        var spki = Base64.getDecoder().decode(pubB64);
        var sig  = Base64.getUrlDecoder().decode(result.signature());
        assertThat(VERIFIER.verify(spki, payload, sig)).isTrue();
    }

    @Test
    void configuredModeExposesConfiguredPublicKeyInBootstrapEndpoint() throws Exception {
        var kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        var privB64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        var pubB64  = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

        var service = buildService(privB64, pubB64);

        var keys = service.listActivePublicKeys();
        assertThat(keys.activeKeyId()).isEqualTo("test-key-id");
        assertThat(keys.keys()).hasSize(1);
        assertThat(keys.keys().get(0).publicKey()).isEqualTo(pubB64);
        assertThat(keys.keys().get(0).algorithm()).isEqualTo("ED25519");
        assertThat(keys.keys().get(0).status()).isEqualTo("ACTIVE");
    }

    // ── Startup validation ────────────────────────────────────────────────────

    @Test
    void failsFastForProdProfileWhenKeysAbsent() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        assertThatThrownBy(() -> buildService(null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("prod");
    }

    @Test
    void failsFastForStagingProfileWhenKeysAbsent() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});

        assertThatThrownBy(() -> buildService(null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("staging");
    }

    @Test
    void failsFastOnPartialConfig_privateOnlyNoPublic() throws Exception {
        var kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        var privB64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());

        assertThatThrownBy(() -> buildService(privB64, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("BOTH");
    }

    @Test
    void failsFastOnPartialConfig_publicOnlyNoPrivate() throws Exception {
        var kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        var pubB64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

        assertThatThrownBy(() -> buildService(null, pubB64))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("BOTH");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Ed25519ServerSigningService buildService(String privB64, String pubB64) {
        var props = new KeyManagementProperties(
            new KeyManagementProperties.ServerSigning("test-key-id", "ED25519", privB64, pubB64));
        var service = new Ed25519ServerSigningService(props, environment);
        service.init();
        return service;
    }
}
