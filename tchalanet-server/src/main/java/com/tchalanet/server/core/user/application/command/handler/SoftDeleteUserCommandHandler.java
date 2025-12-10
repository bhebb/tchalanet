package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class SoftDeleteUserCommandHandler {

    private final UserWriterPort repo;

    @Transactional
    public void softDelete(UUID userId) {
        repo.softDelete(userId, Instant.now());
    }
}
