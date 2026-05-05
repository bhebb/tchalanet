package com.tchalanet.server.core.notification.infra.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class EdgeHmacSignerTest {

    private ObjectMapper objectMapper;
    private Clock clock;
    private EdgeHmacSigner signer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        clock = Clock.fixed(Instant.parse("2026-05-04T12:00:00Z"), ZoneId.of("UTC"));
        signer = new EdgeHmacSigner(objectMapper, clock);
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

