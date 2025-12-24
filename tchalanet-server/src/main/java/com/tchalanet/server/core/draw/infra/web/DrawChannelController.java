package com.tchalanet.server.core.draw.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.draw.application.command.model.CreateDrawChannelCommand;
import com.tchalanet.server.core.draw.application.command.model.UpdateDrawChannelCommand;
import com.tchalanet.server.core.draw.application.query.model.GetDrawChannelQuery;
import com.tchalanet.server.core.draw.application.query.model.ListActiveDrawChannelsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListDrawChannelsQuery;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/draw-channels")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class DrawChannelController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final DrawChannelWebMapper mapper;

    @GetMapping
    public List<DrawChannelSummaryResponse> list(
        @RequestParam UUID tenantId, @RequestParam(required = false) Boolean activeOnly) {
        if (activeOnly == null) {
            List<DrawChannelSummary> channels = queryBus.send(new ListDrawChannelsQuery(tenantId, null));
            return channels.stream().map(mapper::toSummaryResponse).collect(Collectors.toList());
        } else if (activeOnly) {
            List<DrawChannelSummary> channels = queryBus.send(new ListActiveDrawChannelsQuery(tenantId));
            return channels.stream().map(mapper::toSummaryResponse).collect(Collectors.toList());
        } else {
            List<DrawChannelSummary> channels = queryBus.send(new ListDrawChannelsQuery(tenantId, false));
            return channels.stream().map(mapper::toSummaryResponse).collect(Collectors.toList());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DrawChannelResponse> get(
        @PathVariable UUID id, @RequestParam UUID tenantId) {
        try {
            DrawChannel channel = queryBus.send(new GetDrawChannelQuery(tenantId, id));
            return ResponseEntity.ok(mapper.toResponse(channel));
        } catch (IllegalArgumentException ignored) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<DrawChannelResponse> create(@RequestBody CreateDrawChannelRequest request) {
        var command = mapper.toCreateCommand(request);
        var saved = commandBus.send(command);
        return ResponseEntity.ok(mapper.toResponse(saved));
    }

    @PutMapping
    public ResponseEntity<DrawChannelResponse> update(@RequestBody UpdateDrawChannelRequest request) {
        var command = mapper.toUpdateCommand(request);
        var updated = commandBus.send(command);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
