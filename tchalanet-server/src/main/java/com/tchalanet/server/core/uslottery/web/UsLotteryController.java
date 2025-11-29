package com.tchalanet.server.core.uslottery.web;

import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.uslottery.domain.ports.in.RefreshUsLotteryResultsUseCase;
import com.tchalanet.server.core.uslottery.web.dto.RefreshRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/uslottery")
@RequiredArgsConstructor
@Slf4j
public class UsLotteryController {

  private final RefreshUsLotteryResultsUseCase refreshUsLotteryResultsUseCase;

  @PostMapping("/refresh")
  @RequiresPermission(
      "uslottery.refresh_results") // Requires permission to manually trigger refresh
  public ResponseEntity<String> refreshResults(
      @RequestHeader("X-Tenant-Id") UUID tenantId, // Assuming tenantId is passed in header
      @RequestHeader("X-User-Id") UUID userId, // Assuming userId is passed in header
      @Valid @RequestBody RefreshRequest request) {
    // The tenantId and userId from the header/body can be used for context if needed by the use
    // case
    // For now, RefreshUsLotteryResultsUseCase.refreshAllProviders() does not take tenantId/userId
    // but it could be adapted if the refresh needs to be tenant-specific.
    refreshUsLotteryResultsUseCase.refresh();
    log.info(
        "Manual refresh of US Lottery results triggered by user {} for tenant {}",
        userId,
        tenantId);
    return ResponseEntity.ok("US Lottery results refresh triggered successfully.");
  }
}
