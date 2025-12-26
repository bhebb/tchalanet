package com.tchalanet.server.core.audit.infra.config;

import com.tchalanet.server.common.context.TchRequestContextHolder;
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

  private final TchRequestContextHolder ctxHolder;

  /**
   * Retourne l'UUID de l'utilisateur courant pour l'auditing Spring Data. Si le context est absent
   * ou le sub n'est pas un UUID valide, retourne Optional.empty().
   */
  @Override
  public Optional<UUID> getCurrentAuditor() {
    log.trace("calling getCurrentAuditor()");
    var ctx = ctxHolder.get();
    if (ctx == null || ctx.userId() == null) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(ctx.userUuid());
    } catch (IllegalArgumentException ex) {
      // invalid sub in token/context; log at debug to help investigations but don't spam error logs
      log.info("Invalid user id in request context (not a UUID): {}", ctx.userId(), ex);
      return Optional.empty(); // sub pas en UUID => on ignore
    }
  }
}
