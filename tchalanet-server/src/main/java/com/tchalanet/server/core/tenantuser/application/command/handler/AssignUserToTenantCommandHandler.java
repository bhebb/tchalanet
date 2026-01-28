package com.tchalanet.server.core.tenantuser.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenantuser.application.command.model.AssignUserToTenantCommand;
import com.tchalanet.server.core.tenantuser.application.command.model.AssignUserToTenantResult;
import com.tchalanet.server.core.tenantuser.application.port.out.TenantUserReaderPort;
import com.tchalanet.server.core.tenantuser.application.port.out.TenantUserWriterPort;
import com.tchalanet.server.core.tenantuser.domain.model.TenantUserMembership;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AssignUserToTenantCommandHandler implements CommandHandler<AssignUserToTenantCommand, AssignUserToTenantResult> {

    private final TenantUserReaderPort reader;
    private final TenantUserWriterPort writer;

    @Override
    public AssignUserToTenantResult handle(AssignUserToTenantCommand cmd) {
        var existing = reader.findByTenantIdAndUserId(cmd.tenantId(), cmd.userId());
        boolean isNew = false;
        TenantUserMembership membership;
        if (existing.isPresent()) {
            membership = existing.get();
            membership.assignRole(cmd.roleId());
            membership.changeAutonomy(cmd.autonomyLevel());
            membership.markOwner(cmd.isOwner());
            writer.upsertMembership(membership);
        } else {
            membership = TenantUserMembership.of(cmd.tenantId(), cmd.userId())
                .assignRole(cmd.roleId())
                .changeAutonomy(cmd.autonomyLevel())
                .markOwner(cmd.isOwner());
            writer.upsertMembership(membership);
            isNew = true;
        }

        return new AssignUserToTenantResult(cmd.tenantId(), cmd.userId(), isNew);
    }
}
