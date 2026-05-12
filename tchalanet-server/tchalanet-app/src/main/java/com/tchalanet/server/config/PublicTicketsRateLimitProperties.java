package com.tchalanet.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the rate-limit filter protecting {@code /public/tickets/**}.
 *
 * <p>Keys:
 * <ul>
 *   <li>{@code tch.public.tickets.rate-limit.enabled} — toggle (default: true)</li>
 *   <li>{@code tch.public.tickets.rate-limit.requests-per-second} — refill rate per IP (default: 10)</li>
 *   <li>{@code tch.public.tickets.rate-limit.burst} — initial bucket capacity (default: 30)</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "tch.public.tickets.rate-limit")
@Getter
@Setter
public class PublicTicketsRateLimitProperties {

    private boolean enabled = true;
    private int requestsPerSecond = 10;
    private int burst = 30;
}

