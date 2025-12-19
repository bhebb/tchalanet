package com.tchalanet.server.core.draw.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContextHolder;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
  private final TchRequestContextHolder contextHolder;

  @GetMapping
  public ResponseEntity<List<DrawSummaryResponse>> listDraws(@RequestParam UUID tenantId) {
    List<DrawSummary> summaries = queryBus.send(new ListDrawsQuery(tenantId, null, null, null));
    var responses = summaries.stream().map(mapper::toDrawSummaryResponse).toList();
    return ResponseEntity.ok(responses);
  }

  @PostMapping
  public ResponseEntity<DrawSummaryResponse> createDraw(@RequestBody CreateDrawRequest request) {
    CreateDrawCommand command = mapper.toCreateDrawCommand(request);
    UUID drawId = commandBus.send(command);
    List<DrawSummary> summaries = queryBus.send(new ListDrawsQuery(request.tenantId(), null, null, null));
    Optional<DrawSummaryResponse> summary =
        summaries.stream()
            .filter(s -> s.id().equals(drawId))
            .findFirst()
            .map(mapper::toDrawSummaryResponse);
    return summary
        .map(s -> ResponseEntity.status(201).body(s))
        .orElseGet(() -> ResponseEntity.status(201).body(mapper.toDrawSummaryResponseFallback(request)));
  }

  @PutMapping("/{drawId}")
  public ResponseEntity<DrawSummaryResponse> updateDraw(
      @PathVariable UUID drawId,
      @RequestParam UUID tenantId,
      @RequestBody UpdateDrawRequest request) {
    if (!drawId.equals(request.drawId())) {
      return ResponseEntity.badRequest().build();
    }
    if (!tenantId.equals(request.tenantId())) {
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
      @PathVariable UUID drawId,
      @RequestParam UUID tenantId,
      @RequestBody OverrideDrawResultRequest request) {
    OverrideDrawResultCommand command = mapper.toOverrideDrawResultCommand(request);
    commandBus.send(command);
    return ResponseEntity.ok().build();
  }
}
