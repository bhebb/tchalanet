package com.tchalanet.server.core.uslottery.infra.external;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.tchalanet.server.common.crypto.Hashing;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.application.port.out.ProviderDrawQuery;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderClient;
import com.tchalanet.server.core.uslottery.domain.model.DrawExtras;
import com.tchalanet.server.core.uslottery.domain.model.DrawMain;
import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import com.tchalanet.server.core.uslottery.infra.cache.UsLotteryProviderRawCache;
import com.tchalanet.server.core.uslottery.infra.config.UsLotteryProperties;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.tx",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
public class TexasLatestDrawProviderClient implements UsLotteryProviderClient {

  private static final UsLotteryProvider PROVIDER = UsLotteryProvider.TX;
  private static final String ORIGIN = "TX_RSS";
  private static final String SHAPE = "TX/rss/v2";

  private static final URI DEFAULT_RSS_URI =
      URI.create("https://www.texaslottery.com/export/sites/lottery/rss/tlc_latest.xml");

  // Example: "Pick 3 Morning Winning Numbers for 01/09/2026"
  private static final Pattern TITLE =
      Pattern.compile(
          "^(Pick 3|Daily 4)\\s+(Morning|Day|Evening|Night)\\s+Winning Numbers\\s+for\\s+(\\d{2}/\\d{2}/\\d{4})\\s*$",
          Pattern.CASE_INSENSITIVE);

  // Example desc: "1 - 2 - 3 FIREBALL 4" or "10 - 23 - 04"
  // Accept 3 or 4 numbers, each 1-2 digits, separated by dashes; optional FIREBALL with 1-2 digits
  private static final Pattern DESC =
      Pattern.compile(
          "(\\d{1,2}(?:\\s*-\\s*\\d{1,2}){2,3})\\s*(?:FIREBALL\\s+(\\d{1,2}))?",
          Pattern.CASE_INSENSITIVE);

  private static final DateTimeFormatter MDY = DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.US);

  private final RestClient rest;
  private final UsLotteryProviderRawCache cache;
  private final UsLotteryProperties props;

  public TexasLatestDrawProviderClient(
      @Qualifier("txLotteryRestClient") RestClient rest,
      UsLotteryProviderRawCache cache,
      UsLotteryProperties props) {
    this.rest = Objects.requireNonNull(rest);
    this.cache = Objects.requireNonNull(cache);
    this.props = Objects.requireNonNull(props);
  }

  @Override
  public UsLotteryProvider provider() {
    return PROVIDER;
  }

  @Override
  public List<LatestDraw> fetchDraws(ProviderDrawQuery query) {
    Objects.requireNonNull(query, "query required");

    var cfg = props.getProviders() != null ? props.getProviders().get("tx") : null;

    ZoneId zone =
        ZoneId.of(cfg != null && !blank(cfg.getTimezone()) ? cfg.getTimezone() : "America/Chicago");

    String url = null;
    if (cfg != null && !blank(cfg.getBaseUrl())) {
      url = joinUrl(cfg.getBaseUrl(), cfg.getLatestPath());
    }
    if (blank(url)) url = DEFAULT_RSS_URI.toString();

    // make effectively-final copy for lambda
    final String fetchUrl = url;

    // cache per date
    String queryHash =
        Hashing.sha256Hex(
            "v2|provider=TX|date=" + query.drawDate() + "|shape=" + SHAPE + "|url=" + fetchUrl);

    String body =
        cache.getOrFetch(
            PROVIDER.name(),
            query.drawDate(),
            queryHash,
            () -> {
              try {
                return rest.get().uri(fetchUrl).retrieve().body(String.class);
              } catch (Exception ex) {
                log.warn("tx-client fetch failed date={} err={}", query.drawDate(), ex.toString());
                return null;
              }
            });

    if (blank(body)) return List.of();

    SyndFeed feed = parseFeed(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    if (feed == null || feed.getEntries() == null) return List.of();

    String sourceHash = Hashing.sha256Hex(body);
    List<LatestDraw> out = new ArrayList<>();

    for (SyndEntry e : feed.getEntries()) {
      mapEntry(e, zone, sourceHash).ifPresent(d -> out.add(d));
    }

    // filter date/codes
    return filterAndLimit(out, query);
  }

  private Optional<LatestDraw> mapEntry(SyndEntry e, ZoneId zone, String sourceHash) {
    if (e == null) return Optional.empty();

    String title = safe(e.getTitle()).trim();
    Matcher tm = TITLE.matcher(title);
    if (!tm.matches()) return Optional.empty();

    String gameRaw = tm.group(1).trim().toLowerCase(Locale.ROOT); // pick 3 / daily 4
    String slotRaw = tm.group(2).trim().toLowerCase(Locale.ROOT); // morning/day/evening/night
    String mdy = tm.group(3).trim();

    String externalGameKey = gameRaw.contains("pick 3") ? "PICK3" : "PICK4";
    Slot slot = mapSlot(slotRaw);
    if (slot == null) return Optional.empty();

    LocalDate drawDate;
    try {
      drawDate = LocalDate.parse(mdy, MDY);
    } catch (Exception ex) {
      return Optional.empty();
    }

    String desc = safe(e.getDescription() != null ? e.getDescription().getValue() : "").trim();
    Matcher dm = DESC.matcher(desc);
    if (!dm.find()) return Optional.empty();

    String nums = dm.group(1); // "1 - 2 - 3" or "1 - 2 - 3 - 4"
    String fireball = dm.group(2); // single or two digit
    List<String> digits =
        Arrays.stream(nums.split("-")).map(String::trim).filter(s -> !s.isBlank()).toList();

    // occurredAt = date + drawTime
    OffsetDateTime occurredAt = drawDate.atTime(slot.drawTime).atZone(zone).toOffsetDateTime();

    Instant publishedAt =
        e.getPublishedDate() != null ? e.getPublishedDate().toInstant() : Instant.now();

    DrawExtras extras =
        blank(fireball)
            ? DrawExtras.empty()
            : new DrawExtras(
                List.of(Integer.parseInt(fireball.trim())), Map.of("feature", "FIREBALL"));

    String channelCode = mapChannelCode(externalGameKey, slot);

    int expected = "PICK3".equals(externalGameKey) ? 3 : 4;

    try {
      DrawMain main = new DrawMain(digits);
      main.requireSize(expected, externalGameKey + "_" + slot.name());

      return Optional.of(
          new LatestDraw(
              PROVIDER,
              externalGameKey,
              slot.name(),
              channelCode,
              drawDate,
              occurredAt,
              publishedAt,
              main,
              extras,
              ResultQuality.COMPLETE,
              ORIGIN,
              Map.of("hash", sourceHash, "link", safe(e.getLink()))));
    } catch (Exception ex) {
      return Optional.of(
          new LatestDraw(
              PROVIDER,
              externalGameKey,
              slot.name(),
              channelCode,
              drawDate,
              occurredAt,
              publishedAt,
              new DrawMain(digits),
              extras,
              ResultQuality.SUSPECT,
              ORIGIN,
              Map.of("hash", sourceHash, "link", safe(e.getLink()), "error", ex.getMessage())));
    }
  }

  private enum Slot {
    MORNING("MORNING", LocalTime.of(10, 0)),
    DAY("MIDDAY", LocalTime.of(12, 0)),
    EVENING("EVENING", LocalTime.of(18, 0)),
    NIGHT("EVENING", LocalTime.of(22, 0));

    final String name;
    final LocalTime drawTime;

    Slot(String name, LocalTime drawTime) {
      this.name = name;
      this.drawTime = drawTime;
    }
  }

  private static Slot mapSlot(String raw) {
    return switch (raw) {
      case "morning" -> Slot.MORNING;
      case "day" -> Slot.DAY;
      case "evening" -> Slot.EVENING;
      case "night" -> Slot.NIGHT;
      default -> null;
    };
  }

  private static String mapChannelCode(String gameKey, Slot slot) {
    return switch (gameKey) {
      case "PICK3" ->
          switch (slot) {
            case MORNING, DAY -> "US_TX_PICK3_MID";
            case EVENING, NIGHT -> "US_TX_PICK3_EVE";
          };
      case "PICK4" ->
          switch (slot) {
            case MORNING, DAY -> "US_TX_PICK4_MID";
            case EVENING, NIGHT -> "US_TX_PICK4_EVE";
          };
      default -> null;
    };
  }

  private static SyndFeed parseFeed(byte[] bytes) {
    try (var in = new ByteArrayInputStream(bytes);
        var reader = new XmlReader(in)) {
      return new SyndFeedInput().build(reader);
    } catch (Exception ex) {
      return null;
    }
  }

  private static List<LatestDraw> filterAndLimit(List<LatestDraw> all, ProviderDrawQuery query) {
    if (all == null || all.isEmpty()) return List.of();
    var wantedDate = query.drawDate();
    Set<String> wantedCodes = normalizeSet(query.channelCodes());

    var stream = all.stream().filter(Objects::nonNull).filter(d -> wantedDate.equals(d.drawDate()));

    if (!wantedCodes.isEmpty()) {
      stream = stream.filter(d -> wantedCodes.contains(norm(d.channelCode())));
    }

    return stream.limit(Math.max(1, query.maxDraws())).toList();
  }

  private static Set<String> normalizeSet(Set<String> s) {
    if (s == null || s.isEmpty()) return Set.of();
    var out = new LinkedHashSet<String>();
    for (String x : s) out.add(norm(x));
    return out;
  }

  private static String joinUrl(String base, String path) {
    String b = blank(base) ? "" : base.trim();
    String p = blank(path) ? "" : path.trim();
    if (b.endsWith("/") && p.startsWith("/")) return b.substring(0, b.length() - 1) + p;
    if (!b.endsWith("/") && !p.startsWith("/") && !b.isBlank() && !p.isBlank()) return b + "/" + p;
    return b + p;
  }

  private static String norm(String s) {
    return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
  }

  private static boolean blank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }
}
