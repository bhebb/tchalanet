package com.tchalanet.server.draw.domain.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface GetNextDrawsUseCase {
  List<Map<String, Object>> getNextDraws(UUID tenantId);
}
