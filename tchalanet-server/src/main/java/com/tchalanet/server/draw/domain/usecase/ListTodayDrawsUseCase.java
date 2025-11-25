package com.tchalanet.server.draw.domain.usecase;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Récupère les tirages du jour pour un tenant donné. */
public interface ListTodayDrawsUseCase {

  List<Map<String, Object>> listTodayDraws(UUID tenantId, LocalDate date);
}
