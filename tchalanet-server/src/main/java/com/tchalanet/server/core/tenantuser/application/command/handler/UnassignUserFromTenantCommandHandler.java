package com.tchalanet.server.core.tenantuser.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.core.tenantuser.application.command.model.UnassignUserFromTenantCommand;
import com.tchalanet.server.core.tenantuser.application.port.out.TenantUserWriterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UnassignUserFromTenantCommandHandler implements VoidCommandHandler<UnassignUserFromTenantCommand> {

    private final TenantUserWriterPort writer;
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public void handle(UnassignUserFromTenantCommand cmd) {
        writer.softDeleteMembership(cmd.tenantId(), cmd.userId(), timeProvider.nowInstant());
    }
}
