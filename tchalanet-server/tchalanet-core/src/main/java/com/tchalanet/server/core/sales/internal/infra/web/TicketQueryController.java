package com.tchalanet.server.core.sales.internal.infra.web;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.sales.api.query.GetTicketDetailsQuery;
import com.tchalanet.server.core.sales.internal.infra.web.mapper.TicketWebMapper;
import com.tchalanet.server.core.sales.internal.infra.web.model.TicketResponse;
import com.tchalanet.server.core.sales.internal.infra.web.model.TicketSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN') or hasAuthority('ACTOR_SELLER_TERMINAL')")
@Tag(name = "Sales • Ticket Queries", description = "Read endpoints for ticket listing and details")
public class TicketQueryController {

  private final QueryBus queryBus;
  private final TicketWebMapper mapper;

  @Operation(
      operationId = "listTickets",
      summary = "List tickets with filters",
      description = "Returns paginated ticket summaries for the current tenant with optional filters.")
  @io.swagger.v3.oas.annotations.responses.ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tickets listed"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid filters"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<TchPage<TicketSummaryResponse>> list(
      @RequestParam(required = false) TerminalId terminalId,
      @RequestParam(required = false) DrawId drawId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @TchPaging(
              allowedSort = {"createdAt", "totalAmount", "ticketCode"},
              defaultSort = {"createdAt,DESC"})
          TchPageRequest pageReq) {
    var query =
        mapper.toListTicketsQuery(terminalId, drawId, status, from, to, pageReq);
    var result = queryBus.ask(query);
    return ApiResponse.success(mapper.toPagedSummaryResponse(result));
  }

  @Operation(
      operationId = "getTicketDetails",
      summary = "Get ticket details",
      description = "Returns detailed ticket information by ticket id.")
  @io.swagger.v3.oas.annotations.responses.ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket found"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found")
  })
  @GetMapping("/{ticketId}")
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<TicketResponse> details(@PathVariable TicketId ticketId) {
    var result = queryBus.ask(new GetTicketDetailsQuery(ticketId));
    return ApiResponse.success(mapper.toTicketResponse(result));
  }
}
