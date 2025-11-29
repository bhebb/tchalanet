package com.tchalanet.server.core.draw.infra.web;

import com.tchalanet.server.core.draw.application.port.in.command.CreateDrawChannelCommandHandler;
import com.tchalanet.server.core.draw.application.port.in.command.UpdateDrawChannelCommandHandler;
import com.tchalanet.server.core.draw.application.port.in.query.GetDrawChannelQueryHandler;
import com.tchalanet.server.core.draw.application.port.in.query.ListActiveDrawChannelsQueryHandler;
import com.tchalanet.server.core.draw.application.port.in.query.ListDrawChannelsQueryHandler;
import com.tchalanet.server.core.draw.application.query.model.GetDrawChannelQuery;
import com.tchalanet.server.core.draw.application.query.model.ListActiveDrawChannelsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListDrawChannelsQuery;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.infra.web.mapper.DrawChannelWebMapper;
import com.tchalanet.server.core.draw.infra.web.model.CreateDrawChannelRequest;
import com.tchalanet.server.core.draw.infra.web.model.DrawChannelResponse;
import com.tchalanet.server.core.draw.infra.web.model.DrawChannelSummaryResponse;
import com.tchalanet.server.core.draw.infra.web.model.UpdateDrawChannelRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/draw-channels")
@RequiredArgsConstructor
public class DrawChannelController {

  private final CreateDrawChannelCommandHandler createDrawChannelCommandHandler;
  private final UpdateDrawChannelCommandHandler updateDrawChannelCommandHandler;
  private final GetDrawChannelQueryHandler getDrawChannelQueryHandler;
  private final ListDrawChannelsQueryHandler listDrawChannelsQueryHandler;
  private final ListActiveDrawChannelsQueryHandler listActiveDrawChannelsQueryHandler;

  private final DrawChannelWebMapper mapper;

  @GetMapping
  public List<DrawChannelSummaryResponse> list(
      @RequestParam UUID tenantId, @RequestParam(required = false) Boolean activeOnly) {
    if (activeOnly == null) {
      return listDrawChannelsQueryHandler.list(new ListDrawChannelsQuery(tenantId, null)).stream()
          .map(mapper::toSummaryResponse)
          .collect(Collectors.toList());
    } else if (activeOnly) {
      return listActiveDrawChannelsQueryHandler
          .listActive(new ListActiveDrawChannelsQuery(tenantId))
          .stream()
          .map(mapper::toSummaryResponse)
          .collect(Collectors.toList());
    } else {
      return listDrawChannelsQueryHandler.list(new ListDrawChannelsQuery(tenantId, false)).stream()
          .map(mapper::toSummaryResponse)
          .collect(Collectors.toList());
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<DrawChannelResponse> get(
      @PathVariable UUID id, @RequestParam UUID tenantId) {
    try {
      DrawChannel channel = getDrawChannelQueryHandler.get(new GetDrawChannelQuery(tenantId, id));
      return ResponseEntity.ok(mapper.toResponse(channel));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  public ResponseEntity<DrawChannelResponse> create(@RequestBody CreateDrawChannelRequest request) {
    var command = mapper.toCreateCommand(request);
    var saved = createDrawChannelCommandHandler.handle(command);
    return ResponseEntity.ok(mapper.toResponse(saved));
  }

  @PutMapping("/{id}")
  public ResponseEntity<DrawChannelResponse> update(
      @PathVariable UUID id, @RequestBody UpdateDrawChannelRequest request) {
    var command = mapper.toUpdateCommand(request);
    var updated = updateDrawChannelCommandHandler.handle(command);
    return ResponseEntity.ok(mapper.toResponse(updated));
  }
}
