package com.tchalanet.server.core.draw.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.query.model.GetDrawResultQuery;
import com.tchalanet.server.core.draw.application.query.model.ListDrawResultsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListLastDaysDrawResultsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListTodayDrawResultQuery;
import com.tchalanet.server.core.draw.infra.web.mapper.DrawResultWebMapper;
import com.tchalanet.server.core.draw.infra.web.model.DrawResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/draw-results")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Admin • Draw Results")
public class DrawResultsController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final DrawResultWebMapper mapper;

  @Operation(summary = "Get draw result (admin)")
  @GetMapping("/{drawId}")
  public ResponseEntity<DrawResultResponse> get(
      @PathVariable DrawId drawId, @RequestParam TenantId tenantId) {
    var result = queryBus.send(new GetDrawResultQuery(tenantId, drawId));
    return ResponseEntity.ok(mapper.toResponse(result));
  }

  @Operation(summary = "List draw results (admin)")
  @GetMapping
  public List<DrawResultResponse> list(
      @RequestParam TenantId tenantId,
      @RequestParam String channelCode,
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    return queryBus
        .send(new ListDrawResultsQuery(tenantId, channelCode, from, to, page, size))
        .stream()
        .map(mapper::toResponse)
        .collect(Collectors.toList());
  }

  @Operation(summary = "List today's draw results (admin)")
  @GetMapping("/today")
  public List<DrawResultResponse> listToday(
      @RequestParam TenantId tenantId,
      @RequestParam String channelCode,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    return queryBus.send(new ListTodayDrawResultQuery(tenantId, channelCode, page, size)).stream()
        .map(mapper::toResponse)
        .collect(Collectors.toList());
  }

  @Operation(summary = "List draw results for last days (admin)")
  @GetMapping("/last-days")
  public List<DrawResultResponse> listLastDays(
      @RequestParam TenantId tenantId,
      @RequestParam String channelCode,
      @RequestParam int days,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    return queryBus
        .send(new ListLastDaysDrawResultsQuery(tenantId, channelCode, days, page, size))
        .stream()
        .map(mapper::toResponse)
        .collect(Collectors.toList());
  }
}
