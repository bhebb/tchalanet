package com.tchalanet.server.features.stats.web;

import com.tchalanet.server.features.stats.domain.ports.in.GetDrawStatsQuery;
import com.tchalanet.server.features.stats.domain.ports.in.GetTenantDailyStatsQuery;
import com.tchalanet.server.features.stats.web.dto.DrawStatsResponse;
import com.tchalanet.server.features.stats.web.dto.TenantDailyStatsResponse;
import com.tchalanet.server.features.stats.web.mapper.StatsWebMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/stats")
@RequiredArgsConstructor
public class StatsController {

  private final GetDrawStatsQuery getDrawStatsQuery;
  private final GetTenantDailyStatsQuery getTenantDailyStatsQuery;
  private final StatsWebMapper mapper;

  @GetMapping("/draws/{drawId}")
  public ResponseEntity<DrawStatsResponse> getDrawStats(
      @PathVariable UUID tenantId, @PathVariable UUID drawId) {
    var stats =
        getDrawStatsQuery
            .getStatsForDraw(drawId)
            .filter(ds -> ds.tenantId().equals(tenantId)) // Security check
            .map(mapper::toDrawStatsResponse)
            .orElseThrow(
                () -> new StatsNotFoundException("Draw stats not found for drawId: " + drawId));
    return ResponseEntity.ok(stats);
  }

  @GetMapping("/daily")
  public ResponseEntity<List<TenantDailyStatsResponse>> getTenantDailyStats(
      @PathVariable UUID tenantId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
    List<TenantDailyStatsResponse> stats =
        getTenantDailyStatsQuery.getDailyStatsForTenant(tenantId, from, to).stream()
            .map(mapper::toTenantDailyStatsResponse)
            .collect(Collectors.toList());
    return ResponseEntity.ok(stats);
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  public static class StatsNotFoundException extends RuntimeException {
    public StatsNotFoundException(String message) {
      super(message);
    }
  }
}
