package com.tchalanet.server.features.publicdraw.app;

import com.tchalanet.server.features.publicdraw.PublicDrawResultMapper;
import com.tchalanet.server.features.publicdraw.model.PublicDrawResultDetailsResponse;
import com.tchalanet.server.features.publicdraw.persistence.PublicDrawResultRow;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicDrawResultDetailsService {

  private final PublicDrawResultReader reader;
  private final PublicDrawResultMapper mapper;
  private final NextDrawCalculator nextDrawCalculator;

  public PublicDrawResultDetailsResponse get(String slotKey, LocalDate drawDate) {
    PublicDrawResultRow row =
        reader.findOne(slotKey, drawDate)
            .orElseThrow(() -> new IllegalArgumentException("public draw result not found"));

    var item = mapper.toItem(row);

    Instant next =
        nextDrawCalculator.nextScheduledAt(
            row.getSlotTimezone(), row.getSlotDrawTime(), row.getDaysOfWeek());

    LocalDate nextDate = null;
    String nextTime = null;
    try {
      var zone = ZoneId.of(row.getSlotTimezone());
      nextDate = next == null ? null : next.atZone(zone).toLocalDate();
      nextTime = row.getSlotDrawTime() == null ? null : row.getSlotDrawTime().toString();
    } catch (Exception ignore) {
    }

    return new PublicDrawResultDetailsResponse(item, next, nextDate, nextTime);
  }
}
