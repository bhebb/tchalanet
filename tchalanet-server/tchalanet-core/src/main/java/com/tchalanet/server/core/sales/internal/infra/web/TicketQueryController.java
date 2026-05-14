package com.tchalanet.server.core.sales.internal.infra.web;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.sales.api.query.GetTicketDetailsQuery;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.core.sales.internal.infra.web.mapper.TicketWebMapper;
import com.tchalanet.server.core.sales.internal.infra.web.model.TicketResponse;
import com.tchalanet.server.core.sales.internal.infra.web.model.TicketSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/tickets")
@RequiredArgsConstructor
@Tag(name = "Tenant • Ticket Queries")
public class TicketQueryController {

  private final QueryBus queryBus;
  private final TicketWebMapper mapper;

  @Operation(summary = "List tickets with filters")
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
  public ApiResponse<TchPage<TicketSummaryResponse>> list(
      @RequestParam(required = false) TerminalId terminalId,
      @RequestParam(required = false) OutletId outletId,
      @RequestParam(required = false) DrawId drawId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @TchPaging(
              allowedSort = {"createdAt", "totalAmount", "ticketCode"},
              defaultSort = {"createdAt,DESC"})
          TchPageRequest pageReq) {
    if (from != null && to != null && to.isBefore(from)) {
      throw ProblemRest.badRequest("created_to must be greater than or equal to created_from");
    }

    ListTicketsQuery query = mapper.toListTicketsQuery(terminalId, drawId, status, from, to, pageReq);
    var result = queryBus.ask(query);
    return ApiResponse.success(mapper.toPagedSummaryResponse(result));
  }

  @Operation(summary = "Get ticket details")
  @GetMapping("/{ticketId}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyAuthority('CASHIER', 'ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<TicketResponse> details(@PathVariable TicketId ticketId) {
    var result = queryBus.ask(new GetTicketDetailsQuery(ticketId));
    if (result == null) {
      throw ProblemRest.notFound("Ticket not found", ticketId);
    }
    return ApiResponse.success(mapper.toTicketResponse(result));
  }
}
