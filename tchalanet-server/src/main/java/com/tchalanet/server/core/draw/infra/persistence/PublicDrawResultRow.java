package com.tchalanet.server.core.draw.infra.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public interface PublicDrawResultRow {
  String getChannelCode();

  String getChannelName();

  ZoneId getChannelTimezone();

  LocalTime getChannelDrawTime();

  LocalDate getDrawDate();

  Instant getOccurredAt();

  String getNumbersMainJson();

  String getNumbersExtraJson();

  String getQuality();

  String getSource();
}
