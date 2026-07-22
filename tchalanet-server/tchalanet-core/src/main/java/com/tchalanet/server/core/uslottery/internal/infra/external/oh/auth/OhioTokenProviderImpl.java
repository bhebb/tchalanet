package com.tchalanet.server.core.uslottery.internal.infra.external.oh.auth;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.core.uslottery.internal.infra.config.UsLotteryProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OhioTokenProviderImpl implements OhioTokenProvider {

  private final UsLotteryProperties props;
  private final OhioAuthClient authClient;
  private final TchTimeProvider timeProvider;
  private final JsonUtils jsonUtils;

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

    return authClient
        .login()
        .map(
            token -> {
              log.info("Received new token from Ohio auth service");
              cached = CachedToken.fromJwt(jsonUtils, token, timeProvider.now());
              return token;
            });
  }

  private record CachedToken(String token, Instant expiresAt) {

    boolean validAt(Instant now) {
      return expiresAt != null && now.isBefore(expiresAt.minusSeconds(300));
    }

    static CachedToken fromJwt(JsonUtils jsonUtils, String token, Instant fallbackNow) {
      var expiresAt =
          JwtExpiryExtractor.extractExp(jsonUtils, token)
              .orElse(fallbackNow.plus(Duration.ofHours(1)));

      return new CachedToken(token, expiresAt);
    }
  }
}
