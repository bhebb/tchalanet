package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.user.application.command.model.ReplaceUserCommand;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.domain.model.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReplaceUserCommandHandler implements CommandHandler<ReplaceUserCommand, AppUser> {

  private final UserReaderPort userReaderPort;

  @Override
  @Transactional
  public AppUser handle(ReplaceUserCommand command) {
    AppUser existing = userReaderPort.findById(command.userId()).orElseThrow(() -> new IllegalArgumentException("User not found"));

    UserStatus status = existing.status();
    try {
      if (command.status() != null) status = UserStatus.valueOf(command.status());
    } catch (IllegalArgumentException ignored) {
      // keep existing status if invalid
    }

    var replaced = new AppUser(
        existing.id(),
        existing.keycloakId(),
        existing.tenantId(),
        command.username(),
        command.email(),
        command.phone(),
        command.firstName(),
        command.lastName(),
        command.displayName(),
        command.avatarUrl(),
        status,
        command.locale(),
        command.timeZone(),
        existing.lastLoginAt()
    );

    AppUser saved = userReaderPort.save(replaced);
    return saved;
  }
}
