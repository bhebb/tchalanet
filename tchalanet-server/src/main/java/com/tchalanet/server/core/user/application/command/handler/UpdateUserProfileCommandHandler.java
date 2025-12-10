package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.app.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.external.ports.KeycloakUserProvisioningPort;
import com.tchalanet.server.core.user.application.command.model.UpdateUserProfileCommand;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateUserProfileCommandHandler implements CommandHandler<UpdateUserProfileCommand, AppUser> {

    private final UserReaderPort userReader;
    private final UserWriterPort userWriter;
    private final KeycloakUserProvisioningPort keycloakIdentityPort;

    @Override
    @Transactional
    public AppUser handle(UpdateUserProfileCommand command) {
        AppUser existing =
            userReader
                .findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String updatedFirstName = command.firstName().orElse(existing.firstName());
        String updatedLastName = command.lastName().orElse(existing.lastName());
        String updatedEmail = command.email().orElse(existing.email());
        String updatedLocale = command.locale().orElse(existing.locale());

        var updated =
            new AppUser(
                existing.id(),
                existing.keycloakId(),
                existing.tenantId(),
                existing.username(),
                updatedEmail,
                existing.phone(),
                updatedFirstName,
                updatedLastName,
                updatedFirstName + " " + updatedLastName,
                existing.avatarUrl(),
                existing.status(),
                updatedLocale,
                existing.timeZone(),
                existing.lastLoginAt());

        var saved = userWriter.save(updated);

        if (existing.keycloakId() != null) {
            keycloakIdentityPort.updateUserProfile(existing.keycloakId(), updatedFirstName, updatedLastName, updatedEmail, updatedLocale);
        }

        return saved;
    }
}
