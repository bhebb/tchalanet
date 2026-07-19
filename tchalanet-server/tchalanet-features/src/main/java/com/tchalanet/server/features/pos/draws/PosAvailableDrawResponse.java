package com.tchalanet.server.features.pos.draws;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** HTTP projection for cashier draw availability. Domain identifier wrappers never cross this boundary. */
public record PosAvailableDrawResponse(
    String drawId,
    String drawChannelId,
    LocalDate drawDate,
    String resultSlotId,
    String resultSlotKey,
    String channelCode,
    String channelLabel,
    List<String> gameCodes,
    String status,
    Instant scheduledAt,
    Instant cutoffAt,
    String providerDate,
    String providerTime,
    String providerTimezone,
    String localDate,
    String localTime,
    String localTimezone) {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  static PosAvailableDrawResponse from(
      PosAvailableDrawView view, ResultSlotCatalog resultSlotCatalog, ZoneId tenantZone) {
    var providerZone =
        view.resultSlotId() == null
            ? ZoneId.of("UTC")
            : resultSlotCatalog
                .findById(view.resultSlotId())
                .map(slot -> slot.timezone())
                .orElse(ZoneId.of("UTC"));
    var localZone = tenantZone != null ? tenantZone : ZoneId.of("UTC");
    var providerSchedule = view.scheduledAt() == null ? null : view.scheduledAt().atZone(providerZone);
    var localSchedule = view.scheduledAt() == null ? null : view.scheduledAt().atZone(localZone);

    return new PosAvailableDrawResponse(
        view.drawId().value().toString(),
        view.drawChannelId().value().toString(),
        view.drawDate(),
        view.resultSlotId() == null ? null : view.resultSlotId().value().toString(),
        view.resultSlotKey(),
        view.channelCode(),
        view.channelLabel(),
        view.gameCodes(),
        view.status(),
        view.scheduledAt(),
        view.cutoffAt(),
        providerSchedule == null ? "" : DATE_FORMATTER.format(providerSchedule),
        providerSchedule == null ? "" : TIME_FORMATTER.format(providerSchedule),
        providerZone.getId(),
        localSchedule == null ? "" : DATE_FORMATTER.format(localSchedule),
        localSchedule == null ? "" : TIME_FORMATTER.format(localSchedule),
        localZone.getId());
  }
}
