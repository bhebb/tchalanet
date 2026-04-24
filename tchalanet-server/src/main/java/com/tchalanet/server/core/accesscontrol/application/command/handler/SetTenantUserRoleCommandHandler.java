package com.tchalanet.server.core.accesscontrol.application.command.handler;


import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.core.accesscontrol.application.command.model.SetTenantUserRoleCommand;
import com.tchalanet.server.core.accesscontrol.infra.persistence.AppRoleJpaRepository;
import com.tchalanet.server.core.tenantuser.application.port.out.TenantUserReaderPort;
import com.tchalanet.server.core.tenantuser.application.port.out.TenantUserWriterPort;
import com.tchalanet.server.core.tenantuser.domain.model.TenantUserMembership;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SetTenantUserRoleCommandHandler implements VoidCommandHandler<SetTenantUserRoleCommand> {

    private final AppRoleJpaRepository roleRepo;
    private final TenantUserWriterPort tenantUserWriter;
    private final TenantUserReaderPort tenantUserReader;

    @Override
    public void handle(SetTenantUserRoleCommand cmd) {
        // Find the tenant role by code (TchRole maps to role code string)
        var roleCode = cmd.role().name();
        var tenantRoleOpt = roleRepo.findTenantRole(cmd.tenantId().value(), roleCode);
        if (tenantRoleOpt.isEmpty()) {
            throw ProblemRest.badRequest("Role not found for tenant: " + roleCode);
        }

        var roleEntity = tenantRoleOpt.get();
        var roleId = RoleId.of(roleEntity.getId());

        // Read existing membership if present to preserve other attributes
        var existingOpt = tenantUserReader.findByUserId(cmd.userId());
        if (existingOpt.isPresent()) {
            var membership = existingOpt.get();
            membership.assignRole(roleId);
            tenantUserWriter.upsertMembership(membership);
        } else {
            var membership = TenantUserMembership.of(cmd.tenantId(), cmd.userId()).assignRole(roleId);
            tenantUserWriter.upsertMembership(membership);
        }
    }
}
