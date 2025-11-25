package com.tchalanet.server.common.audit.infra;

import com.tchalanet.server.common.context.RequestContextHolder;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestContextAuditorAware implements AuditorAware<UUID> {

  private final RequestContextHolder ctxHolder;

  @Override
  public Optional<UUID> getCurrentAuditor() {
    log.trace("calling getCurrentAuditor()");
    var ctx = ctxHolder.get();
    if (ctx == null || ctx.userId() == null || ctx.userId().isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(ctx.userId()));
    } catch (IllegalArgumentException e) {
      log.error("Invalid user id provided in request context");
      return Optional.empty(); // sub pas en UUID => on ignore
    }
  }
}
