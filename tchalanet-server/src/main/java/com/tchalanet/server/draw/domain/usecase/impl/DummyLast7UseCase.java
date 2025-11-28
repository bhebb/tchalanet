package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.domain.usecase.ListLast7DaysResultsUseCase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
public class DummyLast7UseCase implements ListLast7DaysResultsUseCase {
  @Override
  public List<Map<String, Object>> listLast7Days(UUID tenantId) {
    return List.of();
  }
}
