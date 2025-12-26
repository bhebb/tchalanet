package com.tchalanet.server.core.user.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetUserQueryHandler implements QueryHandler<UserId, Optional<AppUser>> {

    private final UserReaderPort repo;

    @Override
    public Optional<AppUser> handle(UUID id) {
        return repo.findById(id);
    }
}
