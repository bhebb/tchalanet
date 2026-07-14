package com.tchalanet.server.core.sales.internal.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.sales.api.config.TicketVisibilityProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TicketVisibilityPolicy")
class TicketVisibilityPolicyTest {

    private static final Instant NOW = Instant.parse("2026-07-13T12:00:00Z");

    @Test
    @DisplayName("ticket placed within visibility window is visible")
    void withinWindow() {
        var policy = policy(90);
        assertThat(policy.isPubliclyVisible(NOW.minus(Duration.ofDays(89)))).isTrue();
    }

    @Test
    @DisplayName("ticket placed beyond visibility window is not visible")
    void beyondWindow() {
        var policy = policy(90);
        assertThat(policy.isPubliclyVisible(NOW.minus(Duration.ofDays(91)))).isFalse();
    }

    @Test
    @DisplayName("null placedAt is not visible")
    void nullPlacedAt() {
        var policy = policy(90);
        assertThat(policy.isPubliclyVisible(null)).isFalse();
    }

    @Test
    @DisplayName("zero visibility days defaults to 90 via properties")
    void zeroDefaultsTo90() {
        var policy = policy(0);
        assertThat(policy.isPubliclyVisible(NOW.minus(Duration.ofDays(89)))).isTrue();
        assertThat(policy.isPubliclyVisible(NOW.minus(Duration.ofDays(91)))).isFalse();
    }

    @Test
    @DisplayName("ticket placed exactly at boundary is visible")
    void exactBoundary() {
        var policy = policy(1);
        assertThat(policy.isPubliclyVisible(NOW.minus(Duration.ofDays(1)))).isTrue();
    }

    private TicketVisibilityPolicy policy(int days) {
        return new TicketVisibilityPolicy(
            new TicketVisibilityProperties(days),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }
}
