package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.domain.usecase.GetNextDrawsUseCase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
public class DummyNextDrawsUseCase implements GetNextDrawsUseCase {
  @Override
  public List<Map<String, Object>> getNextDraws(UUID tenantId) {
    return List.of();
  }
}
