package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.user.application.command.model.ReactivateUserCommand;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ReactivateUserCommandHandler implements CommandHandler<ReactivateUserCommand, Void> {

  private final UserReaderPort userReaderPort;
  private final UserWriterPort userWriterPort;

  @Override
  public Void handle(ReactivateUserCommand command) {
    var user =
        userReaderPort
            .findById(command.userId())
            .orElseThrow(() -> new IllegalStateException("User not found: " + command.userId()));

    var reactivated = user.reactivate();
    userWriterPort.save(reactivated);

    // Optionnel: réactiver aussi l'utilisateur dans Keycloak si tu ajoutes une méthode au port

    return null;
  }
}
