package com.tchalanet.server.common.web.ratelimit;

import com.tchalanet.server.common.config.PublicTicketsRateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based rate-limit filter for {@code /public/tickets/**}.
 *
 * <p>Uses Bucket4j in-memory buckets keyed by client IP.
 * Default: 10 req/s, burst 30. Configurable via
 * {@link PublicTicketsRateLimitProperties}.
 *
 * <p>Responses when blocked: HTTP 429 + {@code Retry-After} header (in seconds).
 * Rejections are logged at WARN for audit/alerting.
 */
@Component
@Order(10)
@Slf4j
public class PublicTicketsRateLimitFilter extends OncePerRequestFilter {

    private static final String PATH_PREFIX = "/public/tickets";

    private final PublicTicketsRateLimitProperties props;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public PublicTicketsRateLimitFilter(PublicTicketsRateLimitProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain chain)
            throws ServletException, IOException {

        if (!props.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (!path.startsWith(PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> buildBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            long retryAfterSeconds = 1L;
            log.warn("Rate limit exceeded ip={} path={}", ip, path);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":\"TOO_MANY_REQUESTS\",\"message\":\"Rate limit exceeded. Please slow down.\"}");
        }
    }

    private Bucket buildBucket() {
        long burst = Math.max(1, props.getBurst());
        long rps = Math.max(1, props.getRequestsPerSecond());
        Bandwidth limit = Bandwidth.builder()
                .capacity(burst)
                .refillGreedy(rps, Duration.ofSeconds(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}

