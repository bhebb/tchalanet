package com.tchalanet.server.common.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "tch_req_01JZABCDEF1234567890",
        "web_01JZABCDEF1234567890",
        "mob_01JZABCDEF1234567890",
        "abcdefgh",
        "12345678",
        "a1b2c3d4e5f6",
        "tch_req_abc-123:def.456"
    })
    void validFormatsAreAccepted(String value) {
        assertThat(RequestId.isValid(value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "short",
        "has spaces",
        "tab\there",
    })
    void invalidFormatsAreRejected(String value) {
        assertThat(RequestId.isValid(value)).isFalse();
    }

    @Test
    void nullIsInvalid() {
        assertThat(RequestId.isValid(null)).isFalse();
    }

    @Test
    void tooLongIsInvalid() {
        assertThat(RequestId.isValid("a".repeat(97))).isFalse();
    }

    @Test
    void exactly96CharsIsValid() {
        assertThat(RequestId.isValid("a".repeat(96))).isTrue();
    }

    @Test
    void exactly8CharsIsValid() {
        assertThat(RequestId.isValid("a".repeat(8))).isTrue();
    }

    @Test
    void exactly7CharsIsInvalid() {
        assertThat(RequestId.isValid("a".repeat(7))).isFalse();
    }
}
