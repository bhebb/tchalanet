package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.app.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.user.application.command.model.UnblockUserCommand;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.domain.model.UserStatus;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UnblockUserCommandHandler implements CommandHandler<UnblockUserCommand, AppUser> {

    private final UserReaderPort userRepo;
    private final UserWriterPort userWriterPort;

    @Override
    @TchTx
    public AppUser handle(UnblockUserCommand command) {
        var existing = userRepo.findById(command.userId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        var updated = new AppUser(
            existing.id(),
            existing.keycloakId(),
            existing.tenantId(),
            existing.username(),
            existing.email(),
            existing.phone(),
            existing.firstName(),
            existing.lastName(),
            existing.displayName(),
            existing.avatarUrl(),
            UserStatus.ACTIVE,
            existing.locale(),
            existing.timeZone(),
            existing.lastLoginAt());
        return userWriterPort.save(updated);
    }
}
