package com.tchalanet.server.core.outlet.internal.infra.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.core.outlet.api.command.AssignUserToOutletCommand;
import com.tchalanet.server.core.outlet.api.command.RemoveUserFromOutletCommand;
import com.tchalanet.server.core.outlet.api.query.ListOutletTerminalsQuery;
import com.tchalanet.server.core.outlet.api.query.ListOutletUsersQuery;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.mapper.OutletAdminWebMapper;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.AssignUserRequest;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.OutletTerminalResponse;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.OutletUserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/outlets")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Outlet • User Assignment Admin")
public class OutletAdminAssignmentController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final OutletAdminWebMapper mapper;

    @GetMapping("/{id}/users")
    public ApiResponse<List<OutletUserResponse>> listUsers(@PathVariable OutletId id) {
        var outlets = queryBus.ask(new ListOutletUsersQuery(id));
        return ApiResponse.success(mapper.toResponses(outlets));
    }

    @PostMapping("/{id}/users")
    @AuditLog(
        entity = AuditEntityType.OUTLET,
        action = AuditAction.OUTLET_USER_ASSIGN,
        idExpression = "#id.value().toString()",
        detailsExpression = "#req")
    public ApiResponse<Void> assignUser(
        @CurrentContext TchRequestContext ctx,
        @PathVariable OutletId id,
        @Valid @RequestBody AssignUserRequest req) {
        commandBus.execute(
            new AssignUserToOutletCommand(
                ctx.tenantIdSafe(), id, req.userId(), ctx.currentUserIdRequired()));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}/users/{userId}")
    @AuditLog(
        entity = AuditEntityType.OUTLET,
        action = AuditAction.OUTLET_USER_REMOVE,
        idExpression = "#id.value().toString()",
        detailsExpression = "#userId")
    public ApiResponse<Void> removeUser(
        @CurrentContext TchRequestContext ctx,
        @PathVariable OutletId id,
        @PathVariable UserId userId) {
        commandBus.execute(
            new RemoveUserFromOutletCommand(
                ctx.tenantIdSafe(), id, userId, ctx.currentUserIdRequired()));
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/terminals")
    public ApiResponse<List<OutletTerminalResponse>> listTerminals(@PathVariable OutletId id) {
        var outletsTerminal = queryBus.ask(new ListOutletTerminalsQuery(id));
        return ApiResponse.success(mapper.toListOutletTerminalResponse(outletsTerminal));
    }
}
