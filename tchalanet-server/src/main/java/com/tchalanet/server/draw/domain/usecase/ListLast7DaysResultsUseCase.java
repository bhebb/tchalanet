package com.tchalanet.server.draw.domain.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ListLast7DaysResultsUseCase {
  List<Map<String, Object>> listLast7Days(UUID tenantId);
}
