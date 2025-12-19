package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenant.application.port.out.TenantDirectory;
import com.tchalanet.server.core.user.application.command.model.EnsureUserExistsForPrincipalCommand;
import com.tchalanet.server.core.user.application.command.model.EnsureUserExistsForPrincipalResult;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class EnsureUserExistsForPrincipalCommandHandler
    implements CommandHandler<EnsureUserExistsForPrincipalCommand, EnsureUserExistsForPrincipalResult> {

  private final UserReaderPort userReaderPort;
  private final UserWriterPort userWriterPort;
  private final TenantDirectory tenantDirectory;

  @Override
  public EnsureUserExistsForPrincipalResult handle(EnsureUserExistsForPrincipalCommand command) {
    var tenantId = tenantDirectory.requireTenantIdByCode(command.tenantCode());

    var existingOpt = userReaderPort.findByKeycloakId(command.keycloakId());
    var now = Instant.now(); // TODO: remplacer par ClockPort commun dès qu’il existe

    if (existingOpt.isPresent()) {
      var user = existingOpt.get();

      if (!user.getTenantId().equals(tenantId)) {
        throw new IllegalStateException("Tenant mismatch for user " + user.getId());
      }

      var updated = user.syncProfile(
          command.username(),
          command.email(),
          command.phone(),
          command.firstName(),
          command.lastName(),
          command.displayName(),
          null, // avatarUrl non fourni dans la command V1
          command.locale(),
          command.timeZone(),
          command.tenantCode()
      ).touchLogin(now);

      var saved = userWriterPort.save(updated);
      return new EnsureUserExistsForPrincipalResult(false, saved.getId());
    }

    var newUser = AppUser.createNew(
        null,
        command.keycloakId(),
        tenantId,
        command.tenantCode(),
        command.username(),
        command.email(),
        command.phone(),
        command.firstName(),
        command.lastName(),
        command.displayName(),
        null, // avatarUrl
        command.locale(),
        command.timeZone(),
        now
    );

    var saved = userWriterPort.save(newUser);
    return new EnsureUserExistsForPrincipalResult(true, saved.getId());
  }
}

