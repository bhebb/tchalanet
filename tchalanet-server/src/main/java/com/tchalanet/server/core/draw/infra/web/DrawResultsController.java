package com.tchalanet.server.core.draw.infra.web;

import com.tchalanet.server.core.draw.application.query.handler.GetDrawResultHandler;
import com.tchalanet.server.core.draw.application.query.handler.ListDrawResultsHandler;
import com.tchalanet.server.core.draw.application.query.handler.ListLastDaysDrawResultsHandler;
import com.tchalanet.server.core.draw.application.query.handler.ListTodayDrawResultsHandler;
import com.tchalanet.server.core.draw.application.query.model.GetDrawResultQuery;
import com.tchalanet.server.core.draw.application.query.model.ListDrawResultsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListLastDaysDrawResultsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListTodayDrawResultQuery;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.infra.web.mapper.DrawResultWebMapper;
import com.tchalanet.server.core.draw.infra.web.model.DrawResultResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/draw-results")
@RequiredArgsConstructor
public class DrawResultsController {

  private final GetDrawResultHandler getDrawResultHandler;
  private final ListDrawResultsHandler listDrawResultsHandler;
  private final ListTodayDrawResultsHandler listTodayDrawResultsHandler;
  private final ListLastDaysDrawResultsHandler listLastDaysDrawResultsHandler;
  private final DrawResultWebMapper mapper;

  @GetMapping("/{drawId}")
  public ResponseEntity<DrawResultResponse> get(
      @PathVariable UUID drawId, @RequestParam UUID tenantId) {
    try {
      DrawResult result = getDrawResultHandler.handle(new GetDrawResultQuery(tenantId, drawId));
      return ResponseEntity.ok(mapper.toResponse(result));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping
  public List<DrawResultResponse> list(
      @RequestParam UUID tenantId,
      @RequestParam String channelCode,
      @RequestParam LocalDate from,
      @RequestParam LocalDate to) {
    return listDrawResultsHandler
        .handle(new ListDrawResultsQuery(tenantId, channelCode, from, to))
        .stream()
        .map(mapper::toResponse)
        .collect(Collectors.toList());
  }

  @GetMapping("/today")
  public List<DrawResultResponse> listToday(
      @RequestParam UUID tenantId, @RequestParam String channelCode) {
    return listTodayDrawResultsHandler
        .handle(new ListTodayDrawResultQuery(tenantId, channelCode))
        .stream()
        .map(mapper::toResponse)
        .collect(Collectors.toList());
  }

  @GetMapping("/last-days")
  public List<DrawResultResponse> listLastDays(
      @RequestParam UUID tenantId, @RequestParam String channelCode, @RequestParam int days) {
    return listLastDaysDrawResultsHandler
        .handle(new ListLastDaysDrawResultsQuery(tenantId, channelCode, days))
        .stream()
        .map(mapper::toResponse)
        .collect(Collectors.toList());
  }
}
