package com.tchalanet.server.features.tenantadmin.tickets;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.tenantadmin.tickets.model.AdminTicketSettlementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin/support visibility on computed settlement variants (per ticket line). Technical variants
 * such as {@code Permuté · 24-way} are exposed here only — not on the seller terminal or the
 * customer receipt. See change {@code supported-bet-options-combinations-v1}.
 */
@RestController
@RequestMapping("/admin/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Tenant Admin • Tickets")
public class AdminTicketSettlementController {

    private final AdminTicketSettlementService service;

    @GetMapping("/{ticketId}/settlement-variants")
    @Operation(summary = "Computed settlement variants per ticket line (admin/support only)")
    public ApiResponse<AdminTicketSettlementResponse> settlementVariants(
        @PathVariable TicketId ticketId
    ) {
        return ApiResponse.success(service.getSettlementVariants(ticketId));
    }
}
