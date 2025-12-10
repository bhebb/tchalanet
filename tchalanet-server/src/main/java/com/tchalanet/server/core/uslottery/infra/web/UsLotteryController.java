package com.tchalanet.server.core.uslottery.infra.web;

import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.uslottery.application.command.handler.RefreshUsLotteryResultsCommandHandler;
import com.tchalanet.server.core.uslottery.infra.web.model.RefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/uslottery")
@RequiredArgsConstructor
@Slf4j
public class UsLotteryController {

  private final RefreshUsLotteryResultsCommandHandler refreshUsLotteryResultsCommandHandler;

  @PostMapping("/refresh")
  @RequiresPermission("uslottery.refresh_results")
  public ResponseEntity<String> refreshResults(@Valid @RequestBody RefreshRequest request) {
    // The tenantId and userId from the header/body can be used for context if needed by the use
    // case
    // For now, RefreshUsLotteryResultsUseCase.refreshAllProviders() does not take tenantId/userId
    // but it could be adapted if the refresh needs to be tenant-specific.
    refreshUsLotteryResultsCommandHandler.refresh();
    log.info(
        "Manual refresh of US Lottery results triggered by user.");
    return ResponseEntity.ok("US Lottery results refresh triggered successfully.");
  }
}
