package com.tchalanet.server.features.cashier.print;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant/cashier/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Cashier • Ticket Print")
public class CashierTicketPrintController {

    private final CashierTicketPrintService service;

    @PostMapping("/{ticketId}/print")
    @Operation(summary = "Render a ticket for print or delivery")
    @PreAuthorize("hasPermission(null, 'ticket.print')")
    @AuditLog(
        entity = AuditEntityType.TICKET,
        action = AuditAction.PRINT_TICKET,
        idExpression = "#ticketId.value().toString()",
        detailsExpression = "#request")
    public ResponseEntity<ByteArrayResource> printTicket(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TicketId ticketId,
        @Valid @RequestBody PrintTicketRequest request
    ) {
        return service.printTicket(ctx, ticketId, request);
    }
}
