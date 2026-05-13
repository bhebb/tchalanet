package com.tchalanet.server.core.sales.internal.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.sales.internal.infra.web.mapper.TicketWebMapper;
import com.tchalanet.server.core.sales.internal.infra.web.model.OverrideTicketResultRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
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
public class AdminTicketController {

  private final CommandBus commandBus;
  private final TicketWebMapper mapper;

  @Operation(summary = "Override a ticket result")
  @PatchMapping("/{ticketId}/result/override")
  @Secured({"ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<Void> overrideResult(
      @CurrentContext TchRequestContext ctx,
      @PathVariable TicketId ticketId, @Valid @RequestBody OverrideTicketResultRequest request) {
    var cmd = mapper.toOverrideTicketResultCommand(ticketId, ctx.userId(), request);
    commandBus.execute(cmd);
    return ApiResponse.success(null);
  }
}
