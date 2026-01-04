package com.tchalanet.server.core.draw.application.print;

import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class DrawChannelLabelResolver {

  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

  public String resolve(DrawChannel channel, Locale locale) {
    if (channel == null) return "—";

    // Base: nom du channel (ex: "NY Pick 3")
    String base = channel.name();

    // Slot: déduit du code (MID/EVE)
    String slot = "";
    if (channel.code() != null) {
      if (channel.code().endsWith("_MID")) {
        slot = isFrench(locale) ? "Midi" : "Midday";
      } else if (channel.code().endsWith("_EVE")) {
        slot = isFrench(locale) ? "Soir" : "Evening";
      }
    }

    // Heure: HH:mm
    String time = "";
    if (channel.drawTime() != null) {
      time = channel.drawTime().format(TIME_FMT);
    }

    // Assemblage: "NY Pick 3 Midi (12:30)"
    var sb = new StringBuilder(base);
    if (!slot.isEmpty()) sb.append(" ").append(slot);
    if (!time.isEmpty()) sb.append(" (").append(time).append(")");

    return sb.toString();
  }

  private boolean isFrench(Locale locale) {
    return locale != null && locale.getLanguage().equals("fr");
  }
}
