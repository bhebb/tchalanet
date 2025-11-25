package com.tchalanet.server.draw.web;

import com.tchalanet.server.draw.domain.usecase.GetNextDrawsUseCase;
import com.tchalanet.server.draw.domain.usecase.ListLast7DaysResultsUseCase;
import com.tchalanet.server.draw.domain.usecase.ListTodayResultsUseCase;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/draws")
@RequiredArgsConstructor
public class PublicDrawsController {

  private final ListTodayResultsUseCase todayUseCase;
  private final ListLast7DaysResultsUseCase last7UseCase;
  private final GetNextDrawsUseCase nextUseCase;

  @GetMapping("/summary")
  public Map<String, Object> summary(@RequestParam UUID tenantId) {
    var today = todayUseCase.listTodayResults(tenantId);
    var last7 = last7UseCase.listLast7Days(tenantId);
    var next = nextUseCase.getNextDraws(tenantId);
    return Map.of(
        "tenantId", tenantId,
        "today", today,
        "last7", last7,
        "next", next);
  }
}
