package com.tchalanet.server.platform.batch.notification;

import com.tchalanet.server.platform.notification.internal.batch.BatchNotification;
import com.tchalanet.server.platform.notification.internal.batch.BatchNotificationCacheSpecProvider;
import com.tchalanet.server.platform.notification.internal.batch.BatchNotificationPolicy;
import com.tchalanet.server.platform.notification.internal.batch.BatchNotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchNotificationPolicyTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private Clock clock;
    private BatchNotificationPolicy policy;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-05-04T12:00:00Z"), ZoneId.of("UTC"));
        policy = new BatchNotificationPolicy(cacheManager, clock);
    }

    @Test
    void shouldNotSend_whenStatusIsStarted() {
        var notification = new BatchNotification(
            "test-job",
            "tenant-1",
            BatchNotificationStatus.STARTED,
            null,
            null,
            "req-1",
            Instant.now(clock),
            null
        );

        assertThat(policy.shouldSend(notification)).isFalse();
    }

    @Test
    void shouldNotSend_whenStatusIsSucceeded() {
        var notification = new BatchNotification(
            "test-job",
            "tenant-1",
            BatchNotificationStatus.SUCCEEDED,
            null,
            null,
            "req-1",
            Instant.now(clock),
            null
        );

        assertThat(policy.shouldSend(notification)).isFalse();
    }

    @Test
    void shouldNotSend_whenSkippedWithOtherCode() {
        var notification = new BatchNotification(
            "test-job",
            "tenant-1",
            BatchNotificationStatus.SKIPPED,
            "other_reason",
            "Some other reason",
            "req-1",
            Instant.now(clock),
            null
        );

        assertThat(policy.shouldSend(notification)).isFalse();
    }

    @Test
    void shouldSend_whenSkippedWithGateDisabled() {
        when(cacheManager.getCache(BatchNotificationCacheSpecProvider.CACHE_NAME))
            .thenReturn(cache);
        when(cache.get(any(String.class), eq(Instant.class)))
            .thenReturn(null);

        var notification = new BatchNotification(
            "test-job",
            "tenant-1",
            BatchNotificationStatus.SKIPPED,
            "gate_disabled",
            "Gate is disabled",
            "req-1",
            Instant.now(clock),
            null
        );

        assertThat(policy.shouldSend(notification)).isTrue();
        verify(cache).put(any(String.class), eq(Instant.now(clock)));
    }

    @Test
    void shouldSend_whenFailed() {
        when(cacheManager.getCache(BatchNotificationCacheSpecProvider.CACHE_NAME))
            .thenReturn(cache);
        when(cache.get(any(String.class), eq(Instant.class)))
            .thenReturn(null);

        var notification = new BatchNotification(
            "test-job",
            "tenant-1",
            BatchNotificationStatus.FAILED,
            "RuntimeException",
            "Something went wrong",
            "req-1",
            Instant.now(clock),
            null
        );

        assertThat(policy.shouldSend(notification)).isTrue();
        verify(cache).put(any(String.class), eq(Instant.now(clock)));
    }

    @Test
    void shouldNotSend_whenInsideCooldown() {
        var lastSent = Instant.parse("2026-05-04T11:45:00Z"); // 15 minutes ago
        when(cacheManager.getCache(BatchNotificationCacheSpecProvider.CACHE_NAME))
            .thenReturn(cache);
        when(cache.get(any(String.class), eq(Instant.class)))
            .thenReturn(lastSent);

        var notification = new BatchNotification(
            "test-job",
            "tenant-1",
            BatchNotificationStatus.FAILED,
            "RuntimeException",
            "Something went wrong",
            "req-1",
            Instant.now(clock),
            null
        );

        assertThat(policy.shouldSend(notification)).isFalse();
    }

    @Test
    void shouldSend_whenCooldownExpired() {
        var lastSent = Instant.parse("2026-05-04T11:15:00Z"); // 45 minutes ago
        when(cacheManager.getCache(BatchNotificationCacheSpecProvider.CACHE_NAME))
            .thenReturn(cache);
        when(cache.get(any(String.class), eq(Instant.class)))
            .thenReturn(lastSent);

        var notification = new BatchNotification(
            "test-job",
            "tenant-1",
            BatchNotificationStatus.FAILED,
            "RuntimeException",
            "Something went wrong",
            "req-1",
            Instant.now(clock),
            null
        );

        assertThat(policy.shouldSend(notification)).isTrue();
        verify(cache).put(any(String.class), eq(Instant.now(clock)));
    }

    @Test
    void shouldSend_whenCacheUnavailable() {
        when(cacheManager.getCache(BatchNotificationCacheSpecProvider.CACHE_NAME))
            .thenReturn(null);

        var notification = new BatchNotification(
            "test-job",
            "tenant-1",
            BatchNotificationStatus.FAILED,
            "RuntimeException",
            "Something went wrong",
            "req-1",
            Instant.now(clock),
            null
        );

        assertThat(policy.shouldSend(notification)).isTrue();
    }
}

