package com.tchalanet.server.core.audit.infra.config;

import com.tchalanet.server.common.context.TchContextResolver;
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

  private final TchContextResolver resolver;

  @Override
  public Optional<UUID> getCurrentAuditor() {
    try {
      var ctx = resolver.currentOrNull();
      if (ctx == null) return Optional.empty();

      var user = ctx.userUuid();
      return Optional.ofNullable(user);

    } catch (Exception e) {
      log.debug("Auditor resolution failed", e);
      return Optional.empty();
    }
  }
}
