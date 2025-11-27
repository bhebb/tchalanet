package com.tchalanet.server.draw.domain.ports.in;

import java.time.LocalDate;
import java.util.UUID;

/** Use case pour programmer les tirages à l'avance. */
public interface ScheduleDrawsUseCase {

  void scheduleForDay(UUID tenantId, LocalDate day);
}
