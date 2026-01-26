package com.tchalanet.server.core.draw.application.print;

import com.tchalanet.server.core.draw.domain.model.Draw;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class DrawOccurrenceLabelResolver {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public String resolve(Draw draw, DrawChannel channel, Locale locale) {
    if (draw == null) return "—";

    // Priorité: draw_date + channel.draw_time + channel.timezone
    if (draw.drawDate() != null && channel != null && channel.drawTime() != null) {
      ZoneId zone = channel.timezone() != null ? channel.timezone() : ZoneId.systemDefault();
      ZonedDateTime zdt = ZonedDateTime.of(draw.drawDate(), channel.drawTime(), zone);
      return FMT.format(zdt);
    }

    // Fallback: scheduledAt
    if (draw.scheduledAt() != null) {
      return FMT.format(draw.scheduledAt());
    }

    return "—";
  }
}
