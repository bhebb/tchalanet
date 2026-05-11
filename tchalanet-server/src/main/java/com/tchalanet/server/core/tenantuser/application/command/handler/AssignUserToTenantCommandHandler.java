package com.tchalanet.server.core.tenantuser.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenantuser.application.command.model.AssignUserToTenantCommand;
import com.tchalanet.server.core.tenantuser.application.command.model.AssignUserToTenantResult;
import com.tchalanet.server.core.tenantuser.application.port.out.TenantUserReaderPort;
import com.tchalanet.server.core.tenantuser.application.port.out.TenantUserWriterPort;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import com.tchalanet.server.core.tenantuser.domain.model.TenantUserMembership;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AssignUserToTenantCommandHandler implements CommandHandler<AssignUserToTenantCommand, AssignUserToTenantResult> {

    private final TenantUserReaderPort reader;
    private final TenantUserWriterPort writer;
    private final TerminalReaderPort terminalReader; // from core.terminal

    @Override
    public AssignUserToTenantResult handle(AssignUserToTenantCommand cmd) {
        // 1) Resolve outlet from terminal if provided
        var resolvedOutletId = cmd.outletId();

        if (cmd.terminalId() != null) {
            // terminalReader.findById returns Optional<Terminal>
            var termOpt = terminalReader.findById(cmd.tenantId(), cmd.terminalId());
            if (termOpt.isEmpty()) {
                throw ProblemRest.badRequest("Terminal not found: " + cmd.terminalId());
            }
            Terminal t = termOpt.get();

            if (t.unregisteredAt() != null) {
                throw ProblemRest.badRequest("Terminal unregistered: " + cmd.terminalId());
            }
            if (t.lockedAt() != null) {
                throw ProblemRest.badRequest("Terminal locked: " + cmd.terminalId());
            }

            var terminalOutletId = t.outletId();
            if (resolvedOutletId != null && !resolvedOutletId.equals(terminalOutletId)) {
                throw ProblemRest.badRequest("id does not belong to outletId");
            }
            resolvedOutletId = terminalOutletId;
        }

        // 2) Upsert membership (existing or new)
        var existing = reader.findByUserId(cmd.userId());

        boolean isNew = false;
        TenantUserMembership membership;

        if (existing.isPresent()) {
            membership = existing.get();
        } else {
            membership = TenantUserMembership.of(cmd.tenantId(), cmd.userId());
            isNew = true;
        }

        // 3) Apply updates
        membership
            .assignRole(cmd.roleId())
            .markOwner(cmd.isOwner())
            .assignWorkplace(resolvedOutletId, cmd.terminalId());

        // 4) Persist
        writer.upsertMembership(membership);

        return new AssignUserToTenantResult(cmd.tenantId(), cmd.userId(), isNew);
    }
}
