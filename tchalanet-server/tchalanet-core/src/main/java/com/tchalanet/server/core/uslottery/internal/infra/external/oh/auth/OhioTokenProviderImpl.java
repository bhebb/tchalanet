package com.tchalanet.server.core.uslottery.internal.infra.external.oh.auth;

import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.core.uslottery.internal.infra.config.UsLotteryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OhioTokenProviderImpl implements OhioTokenProvider {

    private final UsLotteryProperties props;
    private final OhioAuthClient authClient;
    private final TchTimeProvider timeProvider;

    private volatile CachedToken cached;

    @Override
    public Optional<String> bearerToken() {
        var cfg = props.getProviders() == null ? null : props.getProviders().get("oh");
        log.info("Ohio token provider config: {}", cfg);
        if (cfg == null || !cfg.isEnabled()) {
            return Optional.empty();
        }

        if (StringUtils.isNotBlank(cfg.getBearerToken())) {
            return Optional.of(cfg.getBearerToken().trim());
        }

        var current = cached;
        if (current != null && current.validAt(timeProvider.now())) {
            return Optional.of(current.token());
        }

        return authClient.login()
            .map(token -> {
                log.info("Received new token from Ohio auth service: {}", token);
                cached = CachedToken.fromJwt(token, timeProvider.now());
                return token;
            });
    }

    private record CachedToken(String token, Instant expiresAt) {

        boolean validAt(Instant now) {
            return expiresAt != null && now.isBefore(expiresAt.minusSeconds(300));
        }

        static CachedToken fromJwt(String token, Instant fallbackNow) {
            var expiresAt = JwtExpiryExtractor.extractExp(token)
                .orElse(fallbackNow.plus(Duration.ofHours(1)));

            return new CachedToken(token, expiresAt);
        }
    }
}
