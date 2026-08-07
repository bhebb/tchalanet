package com.tchalanet.server.core.uslottery.internal.infra.external.mo;

import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsProviderSourceFlags;
import com.tchalanet.server.core.uslottery.internal.infra.external.ProviderSlotCodeMatcher;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MissouriDrawResultsMapper {

  private static final UsLotteryProvider PROVIDER = UsLotteryProvider.MO;
  private static final String ORIGIN = "MO_HTML";
  private static final Pattern SCRIPT =
      Pattern.compile("<script\\b[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern STYLE =
      Pattern.compile("<style\\b[^>]*>.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern TAG = Pattern.compile("<[^>]+>");
  private static final Pattern DATE_MARKER =
      Pattern.compile(
          "(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),\\s+([A-Za-z]+)\\s+(\\d{1,2})",
          Pattern.CASE_INSENSITIVE);

  public UsLotteryProviderResponse map(
      String html, String gameCode, String sourceHash, String url, UsLotteryProviderQuery query) {

    var normalizedGameCode = normalizeGameCode(gameCode);
    if (normalizedGameCode.isBlank()
        || !query.externalGameCodes().contains(normalizedGameCode)
        || StringUtils.isBlank(html)) {
      return UsLotteryProviderResponse.empty(PROVIDER, query);
    }

    var plain = plainText(html);
    var results = new ArrayList<UsLotteryProviderResult>();
    var markers = DATE_MARKER.matcher(plain);

    while (markers.find()) {
      var drawDate = parseDate(markers.group(2), markers.group(3), query.drawDate().getYear());
      if (drawDate == null || !query.drawDate().equals(drawDate)) {
        continue;
      }

      var end = nextDateStart(plain, markers.end());
      var section = plain.substring(markers.end(), end);
      addSlotResult(
          section, normalizedGameCode, drawDate, "MIDDAY", sourceHash, url, query, results);
      addSlotResult(
          section, normalizedGameCode, drawDate, "EVENING", sourceHash, url, query, results);
    }

    return new UsLotteryProviderResponse(
        PROVIDER,
        query.drawDate(),
        query.drawTime(),
        query.timezone(),
        List.copyOf(results),
        query.includeRaw() ? html : null);
  }

  private void addSlotResult(
      String section,
      String gameCode,
      LocalDate drawDate,
      String slot,
      String sourceHash,
      String url,
      UsLotteryProviderQuery query,
      ArrayList<UsLotteryProviderResult> results) {

    if (!ProviderSlotCodeMatcher.matches(slot, query.providerSlotCode())) {
      return;
    }

    var digits = digitsAfter(section, slot, expectedDigits(gameCode));
    if (digits.isEmpty()) {
      return;
    }

    var extras = wildBallAfter(section, slot, expectedDigits(gameCode));
    var expectedSize = expectedDigits(gameCode);
    var quality = digits.size() == expectedSize ? ResultQuality.COMPLETE : ResultQuality.SUSPECT;

    var metadata = new LinkedHashMap<String, String>();
    metadata.put("provider", PROVIDER.name());
    metadata.put("game_code", gameCode);
    metadata.put("draw_date", drawDate.toString());
    metadata.put("provider_slot_code", slot);
    metadata.put(
        "expected_provider_slot_code", ProviderSlotCodeMatcher.normalize(query.providerSlotCode()));
    if (!extras.isEmpty()) {
      metadata.put("wild_ball", String.join(",", extras));
    }

    var flags = new UsProviderSourceFlags(ORIGIN, sourceHash, url, Map.copyOf(metadata));
    results.add(
        new UsLotteryProviderResult(
            gameCode,
            digits,
            extras,
            quality,
            flags,
            drawDate.atTime(query.drawTime()).atZone(query.timezone()).toInstant(),
            query.includeRaw() ? section : null));
  }

  private static int nextDateStart(String plain, int from) {
    var next = DATE_MARKER.matcher(plain);
    if (next.find(from)) {
      return next.start();
    }
    return plain.length();
  }

  private static List<String> digitsAfter(String section, String slot, int count) {
    var idx = indexOfWord(section, slot);
    if (idx < 0) {
      return List.of();
    }
    var out = new ArrayList<String>();
    var matcher = Pattern.compile("\\b\\d\\b").matcher(section.substring(idx + slot.length()));
    while (matcher.find() && out.size() < count) {
      out.add(matcher.group());
    }
    return List.copyOf(out);
  }

  private static List<String> wildBallAfter(String section, String slot, int count) {
    var idx = indexOfWord(section, slot);
    if (idx < 0) {
      return List.of();
    }
    var tail = section.substring(idx + slot.length());
    var wild = Pattern.compile("\\bWild Ball:\\s*(\\d)\\b", Pattern.CASE_INSENSITIVE).matcher(tail);
    if (wild.find()) {
      return List.of(wild.group(1));
    }

    var digits = Pattern.compile("\\b\\d\\b").matcher(tail);
    var seen = 0;
    while (digits.find()) {
      seen++;
      if (seen == count + 1) {
        return List.of(digits.group());
      }
    }
    return List.of();
  }

  private static int indexOfWord(String section, String word) {
    var matcher =
        Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE)
            .matcher(section);
    return matcher.find() ? matcher.start() : -1;
  }

  private static String plainText(String html) {
    var withoutScripts = SCRIPT.matcher(html).replaceAll(" ");
    var withoutStyles = STYLE.matcher(withoutScripts).replaceAll(" ");
    var withoutTags = TAG.matcher(withoutStyles).replaceAll(" ");
    return withoutTags.replace("&nbsp;", " ").replace("&amp;", "&").replaceAll("\\s+", " ").trim();
  }

  private LocalDate parseDate(String monthRaw, String dayRaw, int year) {
    try {
      var month = month(monthRaw);
      if (month == null) {
        return null;
      }
      return LocalDate.of(year, month, Integer.parseInt(dayRaw));
    } catch (Exception ex) {
      log.warn("mo-client failed to parse date month={} day={} year={}", monthRaw, dayRaw, year);
      return null;
    }
  }

  private static Month month(String raw) {
    var n = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    return switch (n) {
      case "JAN", "JANUARY" -> Month.JANUARY;
      case "FEB", "FEBRUARY" -> Month.FEBRUARY;
      case "MAR", "MARCH" -> Month.MARCH;
      case "APR", "APRIL" -> Month.APRIL;
      case "MAY" -> Month.MAY;
      case "JUN", "JUNE" -> Month.JUNE;
      case "JUL", "JULY" -> Month.JULY;
      case "AUG", "AUGUST" -> Month.AUGUST;
      case "SEP", "SEPT", "SEPTEMBER" -> Month.SEPTEMBER;
      case "OCT", "OCTOBER" -> Month.OCTOBER;
      case "NOV", "NOVEMBER" -> Month.NOVEMBER;
      case "DEC", "DECEMBER" -> Month.DECEMBER;
      default -> null;
    };
  }

  private static String normalizeGameCode(String raw) {
    var n = ProviderSlotCodeMatcher.normalize(raw);
    return switch (n) {
      case "PICK3" -> "PICK3";
      case "PICK4" -> "PICK4";
      default -> "";
    };
  }

  private static int expectedDigits(String gameCode) {
    return "PICK4".equals(gameCode) ? 4 : 3;
  }
}
