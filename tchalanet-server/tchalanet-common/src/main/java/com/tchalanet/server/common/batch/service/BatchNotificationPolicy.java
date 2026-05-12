package com.tchalanet.server.common.batch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Politique de décision pour les notifications batch techniques.
 * Détermine si une notification doit être envoyée en fonction du statut et du cooldown.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchNotificationPolicy {

    private static final Duration DEFAULT_COOLDOWN = Duration.ofMinutes(30);

    private final CacheManager cacheManager;
    private final Clock clock;

    /**
     * Détermine si une notification batch doit être envoyée.
     *
     * @param notification la notification batch à évaluer
     * @return true si la notification doit être envoyée
     */
    public boolean shouldSend(BatchNotification notification) {
        if (!shouldNotifyByStatus(notification)) {
            return false;
        }

        var now = clock.instant();
        var key = fingerprint(notification);

        var cache = cacheManager.getCache(BatchNotificationCacheSpecProvider.CACHE_NAME);
        if (cache == null) {
            log.debug("Cache unavailable, allowing send for: {}", key);
            return true;
        }

        var last = cache.get(key, Instant.class);
        if (last != null && Duration.between(last, now).compareTo(DEFAULT_COOLDOWN) < 0) {
            log.debug("Cooldown active for: {}", key);
            return false;
        }

        cache.put(key, now);
        return true;
    }

    /**
     * Détermine si le statut justifie une notification.
     *
     * STARTED -> jamais
     * SUCCEEDED -> jamais
     * SKIPPED -> seulement si code == gate_disabled
     * FAILED -> toujours
     */
    private boolean shouldNotifyByStatus(BatchNotification n) {
        return switch (n.status()) {
            case FAILED -> true;
            case SKIPPED -> "gate_disabled".equals(n.code());
            case STARTED, SUCCEEDED -> false;
        };
    }

    /**
     * Génère une empreinte unique pour le cooldown.
     * Format: jobKey:tenantId:status:code
     */
    private String fingerprint(BatchNotification n) {
        return String.join(":",
            n.jobKey(),
            n.tenantId() == null ? "GLOBAL" : n.tenantId(),
            n.status().name(),
            n.code() == null ? "none" : n.code()
        );
    }
}

