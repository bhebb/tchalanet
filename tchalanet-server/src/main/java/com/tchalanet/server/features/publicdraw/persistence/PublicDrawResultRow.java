package com.tchalanet.server.features.publicdraw.persistence;

import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public interface PublicDrawResultRow {
  String getSlotKey();

  String getProvider();

  String getSlotTimezone();

  LocalTime getSlotDrawTime();

  String getDaysOfWeek(); // "1,2,3,4,5,6,7" (ou "MON,TUE,...")

  LocalDate getDrawDate();

  Instant getOccurredAt();

  String getHaitiResultJson();

  String getSourceResultJson();

  DrawResultStatus getStatus();

  ResultQuality getQuality();

  String getSource();
}
