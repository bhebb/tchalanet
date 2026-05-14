package com.tchalanet.server.platform.communication.internal.adapter;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.communication.internal.provider.EdgeHmacSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class EdgeHmacSignerTest {

    private JsonUtils jsonUtils;
    private Clock clock;
    private EdgeHmacSigner signer;

    @BeforeEach
    void setUp() {
        jsonUtils = new JsonUtils(JsonMapper.builder().build());
        clock = Clock.fixed(Instant.parse("2026-05-04T12:00:00Z"), ZoneId.of("UTC"));
        signer = new EdgeHmacSigner(jsonUtils, clock);
    }

    @Test
    void shouldProduceDeterministicSignature() {
        var secret = "test-secret-key";
        var request = new TestRequest("test-value", 123);

        var result = signer.sign(secret, request);

        assertThat(result.timestamp()).isEqualTo("2026-05-04T12:00:00Z");
        assertThat(result.signature()).startsWith("sha256=");
        assertThat(result.rawJsonBody()).contains("test-value");
        assertThat(result.rawJsonBody()).contains("123");
    }

    @Test
    void shouldProduceSameSignatureForSameInput() {
        var secret = "test-secret-key";
        var request = new TestRequest("test-value", 123);

        var result1 = signer.sign(secret, request);
        var result2 = signer.sign(secret, request);

        assertThat(result1.signature()).isEqualTo(result2.signature());
        assertThat(result1.rawJsonBody()).isEqualTo(result2.rawJsonBody());
    }

    @Test
    void shouldProduceDifferentSignatureForDifferentSecret() {
        var request = new TestRequest("test-value", 123);

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

        var result1 = signer.sign(secret, request1);
        var result2 = signer.sign(secret, request2);

        assertThat(result1.signature()).isNotEqualTo(result2.signature());
        assertThat(result1.rawJsonBody()).isNotEqualTo(result2.rawJsonBody());
    }

    private record TestRequest(String field1, int field2) {}
}
