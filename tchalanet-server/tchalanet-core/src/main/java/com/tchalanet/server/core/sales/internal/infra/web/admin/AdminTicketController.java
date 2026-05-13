package com.tchalanet.server.core.sales.internal.infra.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.core.sales.internal.infra.web.mapper.TicketWebMapper;
import com.tchalanet.server.core.sales.internal.infra.web.model.OverrideTicketResultRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/tickets")
@RequiredArgsConstructor
@Tag(name = "Tenant • Admin Tickets")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
public class AdminTicketController {

    private final CommandBus commandBus;
    private final TicketWebMapper mapper;

    @Operation(summary = "Override a ticket result")
    @PatchMapping("/{ticketId}/result/override")
    @PreAuthorize("hasPermission('ticket.override.result')")
    @AuditLog(
        entity = AuditEntityType.TICKET,
        action = AuditAction.OVERRIDE_RESULT,
        idExpression = "#ticketId",
        detailsExpression = "#request"
    )
    @ResponseStatus(HttpStatus.OK)
    public void overrideResult(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TicketId ticketId,
        @Valid @RequestBody OverrideTicketResultRequest request
    ) {
        var cmd = mapper.toOverrideTicketResultCommand(ticketId, ctx.userId(), request);
        commandBus.execute(cmd);
    }
}
