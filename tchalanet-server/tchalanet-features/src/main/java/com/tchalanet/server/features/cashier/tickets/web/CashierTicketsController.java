package com.tchalanet.server.features.cashier.tickets.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.features.cashier.tickets.app.CashierTicketPageService;
import com.tchalanet.server.features.cashier.tickets.app.CashierTicketPrintService;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketDetailsResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPageResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPrintResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/cashier/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Cashier • Tickets")
public class CashierTicketsController {

    private final CashierTicketPageService pageService;
    private final CashierTicketPrintService printService;

    @GetMapping
    @PreAuthorize("hasPermission('cashier.ticket.list')")
    public ApiResponse<TchPage<CashierTicketPageResponse>> list(
        @TchPaging(
            allowedSort = {"placedAt", "status"},
            defaultSort = {"placedAt,desc"})
        TchPageRequest page) {

        var result = pageService.listTickets(page.pageable());
        return ApiResponse.success(result);
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("hasPermission('cashier.ticket.read')")
    public ApiResponse<CashierTicketDetailsResponse> get(
        @PathVariable TicketId ticketId) {

        var result = pageService.getDetails(ticketId);
        return ApiResponse.success(result);
    }

    @GetMapping("/{ticketId}/print")
    @PreAuthorize("hasPermission('cashier.ticket.print')")
    public ApiResponse<CashierTicketPrintResponse> printView(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TicketId ticketId) {

        var result = printService.getPrintView(ticketId);
        return ApiResponse.success(result);
    }
}
