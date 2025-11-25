package com.tchalanet.server.draw.domain.usecase;

import com.tchalanet.server.draw.domain.dto.DrawDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ListTodayResultsUseCase {
  List<Map<String, DrawDto>> listTodayResults(UUID tenantId);
}
