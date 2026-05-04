package com.tchalanet.server.common.batch.notification;

import com.tchalanet.server.common.notification.NotificationGatewayPort;
import com.tchalanet.server.common.notification.model.SendNotificationPayload;
import com.tchalanet.server.common.types.enums.NotificationChannel;
import com.tchalanet.server.common.types.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class BatchEventNotificationService {

    private static final Duration DEFAULT_COOLDOWN = Duration.ofMinutes(30);

    private final NotificationGatewayPort sender;
    private final CacheManager cacheManager;
    private final Clock clock;

    private void sendBatchNotification(BatchNotification notice) {
        if (!shouldNotify(notice)) {
            return;
        }

        var now = clock.instant();
        var key = fingerprint(notice);

        var cache = cacheManager.getCache(BatchNotificationCacheSpecProvider.CACHE_NAME);
        if (cache == null) {
            sender.send(toPayload(notice));
            return;
        }

        var last = cache.get(key, Instant.class);
        if (last != null && Duration.between(last, now).compareTo(DEFAULT_COOLDOWN) < 0) {
            return;
        }

        cache.put(key, now);
        sender.send(toPayload(notice));
    }

    private boolean shouldNotify(BatchNotification n) {
        if (n.status() == BatchNotificationStatus.FAILED) {
            return true;
        }

        if (n.status() == BatchNotificationStatus.SKIPPED) {
            return "gate_disabled".equals(n.code());
        }

        return false;
    }

    private SendNotificationPayload toPayload(BatchNotification n) {
        var data = n.details() == null
            ? new HashMap<String, Object>()
            : new HashMap<>(n.details());

        data.put("requestId", n.requestId());
        data.put("occurredAt", n.occurredAt());
        data.put("tenantId", n.tenantId());
        data.put("jobKey", n.jobKey());
        data.put("status", n.status().name());
        data.put("code", n.code());
        data.put("message", n.message());

        return new SendNotificationPayload(
            NotificationType.BATCH_MESSAGE,
            NotificationChannel.SLACK,
            null,
            Locale.ENGLISH,
            data
        );
    }

    private String fingerprint(BatchNotification n) {
        return String.join(":",
            n.jobKey(),
            n.tenantId() == null ? "GLOBAL" : n.tenantId(),
            n.status().name(),
            n.code() == null ? "none" : n.code()
        );
    }

    public void started(String jobKey) {
        sendBatchNotification(new BatchNotification(
            jobKey,
            null,
            BatchNotificationStatus.STARTED,
            null,
            null,
            null,
            clock.instant(),
            null
        ));
    }

    public void skipped(String jobKey, String code, String message) {
        sendBatchNotification(new BatchNotification(
            jobKey,
            null,
            BatchNotificationStatus.SKIPPED,
            code,
            message,
            null,
            clock.instant(),
            null
        ));
    }

    public void succeeded(String jobKey) {
        sendBatchNotification(new BatchNotification(
            jobKey,
            null,
            BatchNotificationStatus.SUCCEEDED,
            null,
            null,
            null,
            clock.instant(),
            null
        ));
    }

    public void failed(String jobKey, Throwable e) {
        sendBatchNotification(new BatchNotification(
            jobKey,
            null,
            BatchNotificationStatus.FAILED,
            e.getClass().getSimpleName(),
            e.getMessage(),
            null,
            clock.instant(),
            null
        ));
    }
}
