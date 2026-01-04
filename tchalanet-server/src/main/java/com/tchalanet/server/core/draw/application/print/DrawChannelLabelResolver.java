package com.tchalanet.server.core.draw.application.print;

import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class DrawChannelLabelResolver {

  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

  public String resolve(DrawChannel channel, Locale locale) {
    if (channel == null) return resolve(null, null, locale);
    return resolve(channel.name(), channel.drawTime(), locale);
  }

  // Nouvelle surcharge: accepte uniquement le nom localisé du channel et l'heure de tirage
  public String resolve(String channelName, LocalTime drawTime, Locale locale) {
    if (channelName == null) return "—";

    // Base: nom du channel (ex: "NY Pick 3")
    String base = localizeSlotInName(channelName, locale);

    // Heure: HH:mm
    String time = "";
    if (drawTime != null) {
      time = drawTime.format(TIME_FMT);
    }

    // Assemblage: "NY Pick 3 Midi (12:30)"
    var sb = new StringBuilder(base);
    if (!time.isEmpty()) sb.append(" (").append(time).append(")");

    return sb.toString();
  }

  private String localizeSlotInName(String name, Locale locale) {
    if (name == null) return null;
    boolean fr = locale != null && "fr".equals(locale.getLanguage());
    if (!fr) return name;

    return name.replace("Evening", "Soir").replace("Midday", "Midi");
  }
}
