package com.tchalanet.server.core.draw.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.command.model.CreateDrawCommand;
import com.tchalanet.server.core.draw.application.command.model.OverrideDrawResultCommand;
import com.tchalanet.server.core.draw.application.command.model.UpdateDrawCommand;
import com.tchalanet.server.core.draw.application.query.model.ListDrawsQuery;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import com.tchalanet.server.core.draw.infra.web.mapper.DrawAdminWebMapper;
import com.tchalanet.server.core.draw.infra.web.model.CreateDrawRequest;
import com.tchalanet.server.core.draw.infra.web.model.DrawSummaryResponse;
import com.tchalanet.server.core.draw.infra.web.model.OverrideDrawResultRequest;
import com.tchalanet.server.core.draw.infra.web.model.UpdateDrawRequest;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/draws")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class DrawAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final DrawAdminWebMapper mapper;
  private final TchContextResolver contextResolver;

  @GetMapping
  public ResponseEntity<List<DrawSummaryResponse>> listDraws() {
    var holder = contextResolver.currentOrNull();
    var tenantId = TenantId.of(holder != null ? holder.tenantUuid() : null);
    List<DrawSummary> summaries = queryBus.send(new ListDrawsQuery(tenantId, null, null, null));
    var responses = summaries.stream().map(mapper::toDrawSummaryResponse).toList();
    return ResponseEntity.ok(responses);
  }

  @PostMapping
  public ResponseEntity<DrawSummaryResponse> createDraw(@RequestBody CreateDrawRequest request) {
    CreateDrawCommand command = mapper.toCreateDrawCommand(request);
    TenantId tid = TenantId.of(request.tenantId());
    com.tchalanet.server.common.types.id.DrawId drawId = commandBus.send(command);
    List<DrawSummary> summaries = queryBus.send(new ListDrawsQuery(tid, null, null, null));
    Optional<DrawSummaryResponse> summary =
        summaries.stream()
            .filter(s -> s.id().equals(drawId))
            .findFirst()
            .map(mapper::toDrawSummaryResponse);
    return summary
        .map(s -> ResponseEntity.status(201).body(s))
        .orElseGet(
            () -> ResponseEntity.status(201).body(mapper.toDrawSummaryResponseFallback(request)));
  }

  @PutMapping("/{drawId}")
  public ResponseEntity<DrawSummaryResponse> updateDraw(
      @PathVariable DrawId drawId,
      @RequestParam TenantId tenantId,
      @RequestBody UpdateDrawRequest request) {
    if (!drawId.uuid().equals(request.drawId())) {
      return ResponseEntity.badRequest().build();
    }
    if (!tenantId.uuid().equals(request.tenantId())) {
      return ResponseEntity.badRequest().build();
    }
    UpdateDrawCommand command = mapper.toUpdateDrawCommand(request);
    commandBus.send(command);
    List<DrawSummary> summaries = queryBus.send(new ListDrawsQuery(tenantId, null, null, null));
    Optional<DrawSummaryResponse> summary =
        summaries.stream()
            .filter(s -> s.id().equals(drawId))
            .findFirst()
            .map(mapper::toDrawSummaryResponse);
    return summary
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.ok(mapper.toDrawSummaryResponseFallback(request)));
  }

  @PostMapping("/{drawId}/override-result")
  public ResponseEntity<Void> overrideResult(
      @PathVariable DrawId drawId,
      @RequestParam TenantId tenantId,
      @RequestBody OverrideDrawResultRequest request) {
    OverrideDrawResultCommand command = mapper.toOverrideDrawResultCommand(request);
    commandBus.send(command);
    return ResponseEntity.ok().build();
  }
}
