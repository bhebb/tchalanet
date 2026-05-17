package com.tchalanet.server.features.ticketverify.infra;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.config.PublicTicketRateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// TODO: migrate to Redis-backed implementation for multi-instance deployment
@Component
public class PublicTicketRateLimiter {

    private final PublicTicketRateLimitProperties props;
    private final ConcurrentHashMap<String, IpBucket> buckets = new ConcurrentHashMap<>();

    public PublicTicketRateLimiter(PublicTicketRateLimitProperties props) {
        this.props = props;
    }

    public void requireAllowed(HttpServletRequest request) {
        if (!props.enabled()) return;

        var ip = resolveClientIp(request);
        var bucket = buckets.computeIfAbsent(ip, k -> new IpBucket(props.burst()));

        if (!bucket.tryConsume(props.requestsPerSecond(), props.burst())) {
            throw ProblemRest.of(HttpStatus.TOO_MANY_REQUESTS, "ticket.verify.rate_limit_exceeded");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class IpBucket {
        private final AtomicLong tokens;
        private final AtomicLong lastRefillNanos;

        IpBucket(int initialTokens) {
            this.tokens = new AtomicLong(initialTokens);
            this.lastRefillNanos = new AtomicLong(System.nanoTime());
        }

        boolean tryConsume(int ratePerSecond, int maxTokens) {
            refill(ratePerSecond, maxTokens);
            long current;
            do {
                current = tokens.get();
                if (current <= 0) return false;
            } while (!tokens.compareAndSet(current, current - 1));
            return true;
        }

        private void refill(int ratePerSecond, int maxTokens) {
            long now = System.nanoTime();
            long last = lastRefillNanos.get();
            long elapsed = now - last;
            long nanosPerToken = 1_000_000_000L / Math.max(1, ratePerSecond);
            long newTokens = elapsed / nanosPerToken;
            if (newTokens > 0 && lastRefillNanos.compareAndSet(last, last + newTokens * nanosPerToken)) {
                long updated = Math.min(maxTokens, tokens.get() + newTokens);
                tokens.set(updated);
            }
        }
    }
}
