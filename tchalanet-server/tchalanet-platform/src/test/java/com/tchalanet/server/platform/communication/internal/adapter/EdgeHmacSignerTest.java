package com.tchalanet.server.platform.communication.internal.adapter;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.communication.internal.provider.EdgeHmacSigner;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdgeHmacSignerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-04T12:00:00Z");

    @Mock
    private JsonUtils jsonUtils;

    @Mock
    private Clock clock;

    private EdgeHmacSigner signer;


    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        signer = new EdgeHmacSigner(jsonUtils, clock);
    }

    @Test
    void shouldProduceDeterministicSignature() {
        var secret = "test-secret-key";
        var request = new TestRequest("test-value", 123);
        var rawJson = "{\"field1\":\"test-value\",\"field2\":123}";
        when(jsonUtils.toJson(request)).thenReturn(rawJson);

        var result = signer.sign(secret, request);
        var expectedSignature = expectedSignature(secret, result.timestamp() + "." + rawJson);

        assertThat(result.timestamp()).isEqualTo("2026-05-04T12:00:00Z");
        assertThat(result.rawJsonBody()).isEqualTo(rawJson);
        assertThat(result.signature()).isEqualTo(expectedSignature);
        verify(jsonUtils).toJson(request);
        verify(clock).instant();
    }

    @Test
    void shouldProduceSameSignatureForSameInput() {
        var secret = "test-secret-key";
        var request = new TestRequest("test-value", 123);
        var rawJson = "{\"field1\":\"test-value\",\"field2\":123}";
        when(jsonUtils.toJson(request)).thenReturn(rawJson);

        var result1 = signer.sign(secret, request);
        var result2 = signer.sign(secret, request);

        assertThat(result1.signature()).isEqualTo(result2.signature());
        assertThat(result1.rawJsonBody()).isEqualTo(result2.rawJsonBody());
        verify(jsonUtils, times(2)).toJson(request);
        verify(clock, times(2)).instant();
    }

    @Test
    void shouldProduceDifferentSignatureForDifferentSecret() {
        var request = new TestRequest("test-value", 123);
        var rawJson = "{\"field1\":\"test-value\",\"field2\":123}";
        when(jsonUtils.toJson(request)).thenReturn(rawJson);

        var result1 = signer.sign("secret1", request);
        var result2 = signer.sign("secret2", request);

        assertThat(result1.signature()).isNotEqualTo(result2.signature());
        assertThat(result1.rawJsonBody()).isEqualTo(result2.rawJsonBody());
    }

    @Test
    void shouldProduceDifferentSignatureForDifferentBody() {
        var secret = "test-secret-key";
        var request1 = new TestRequest("value1", 123);
        var request2 = new TestRequest("value2", 456);
        when(jsonUtils.toJson(request1)).thenReturn("{\"field1\":\"value1\",\"field2\":123}");
        when(jsonUtils.toJson(request2)).thenReturn("{\"field1\":\"value2\",\"field2\":456}");

        var result1 = signer.sign(secret, request1);
        var result2 = signer.sign(secret, request2);

        assertThat(result1.signature()).isNotEqualTo(result2.signature());
        assertThat(result1.rawJsonBody()).isNotEqualTo(result2.rawJsonBody());
    }

    private String expectedSignature(String secret, String payloadToSign) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            var signatureBytes = mac.doFinal(payloadToSign.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + toHex(signatureBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute expected signature", e);
        }
    }

    private String toHex(byte[] bytes) {
        var chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            chars[i * 2] = Character.forDigit(value >>> 4, 16);
            chars[i * 2 + 1] = Character.forDigit(value & 0x0F, 16);
        }
        return new String(chars);
    }

    private record TestRequest(String field1, int field2) {
    }
}
