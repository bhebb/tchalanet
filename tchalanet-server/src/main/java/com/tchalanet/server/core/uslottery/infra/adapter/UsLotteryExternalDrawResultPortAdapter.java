package com.tchalanet.server.core.uslottery.infra.adapter;

import com.tchalanet.server.core.draw.application.port.out.ExternalDrawResultPort;
import com.tchalanet.server.core.uslottery.application.port.out.LatestDrawProviderClient;
import com.tchalanet.server.core.uslottery.domain.model.DrawExtras;
import com.tchalanet.server.core.uslottery.domain.model.DrawMain;
import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import com.tchalanet.server.core.uslottery.infra.config.UsLotteryGameRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Primary
@Slf4j
@RequiredArgsConstructor
public class UsLotteryExternalDrawResultPortAdapter implements ExternalDrawResultPort {

  private final List<LatestDrawProviderClient> providers;
  private final UsLotteryGameRegistry registry;

  @Override
  public ExternalDrawResult fetchExternalResult(DrawExternalQuery query) {
    if (query == null) {
      return ExternalDrawResult.notFound("INVALID_QUERY", Map.of());
    }

    String channelCode = safe(query.channelCode());
    if (channelCode.isBlank()) {
      return ExternalDrawResult.notFound("MISSING_CHANNEL", Map.of());
    }

    var infoOpt = registry.resolve(channelCode);
    if (infoOpt.isEmpty()) {
      return ExternalDrawResult.notFound("UNKNOWN_CHANNEL", Map.of("channel_code", channelCode));
    }
    var info = infoOpt.get();

    for (LatestDrawProviderClient provider : providers) {
      if (!provider.provider().name().equalsIgnoreCase(info.provider())) {
        continue;
      }
      try {
        List<LatestDraw> latest = provider.fetchLatestDraws();
        if (latest == null || latest.isEmpty()) continue;

        Optional<LatestDraw> match =
            latest.stream()
                .filter(d -> channelCode.equalsIgnoreCase(safe(d.channelCode())))
                .filter(d -> query.drawDateLocal().equals(d.drawDate()))
                .findFirst();

        if (match.isPresent()) {
          LatestDraw d = match.get();
          Instant occurredAt = d.occurredAtUtc() != null ? d.occurredAtUtc().toInstant() : null;

          Map<String, Object> raw = new LinkedHashMap<>();
          raw.put("provider", provider.provider().name());
          raw.put("external_key", info.externalKey());
          raw.put("origin", d.origin());
          raw.put("channel_code", d.channelCode());
          raw.put("draw_date", String.valueOf(d.drawDate()));

          return ExternalDrawResult.found(
              "FOUND", toStrings(d.numbers()), toStrings(d.extras()), occurredAt, d.quality(), raw);
        }
      } catch (Exception e) {
        log.warn("uslottery-adapter: provider {} failed: {}", provider.provider(), e.toString());
      }
    }

    return ExternalDrawResult.notFound("NOT_FOUND", Map.of("channel_code", channelCode));
  }

  @Override
  public Map<String, ExternalDrawResult> fetchExternalResults(DrawExternalBulkQuery query) {
    if (query == null) return Map.of();

    var channelCodes =
        Optional.ofNullable(query.channelCodes()).orElse(List.of()).stream()
            .filter(Objects::nonNull)
            .map(s -> s.trim().toUpperCase(Locale.ROOT))
            .filter(s -> !s.isBlank())
            .distinct()
            .toList();

    if (channelCodes.isEmpty()) return Map.of();

    // 1) Resolve channel -> provider
    record Resolved(String channelCode, UsLotteryGameRegistry.GameInfo info) {}
    var resolved = new ArrayList<Resolved>();

    for (String code : channelCodes) {
      var infoOpt = registry.resolve(code);
      if (infoOpt.isEmpty()) {
        log.debug("uslottery-adapter: unknown channel {}", code);
        continue;
      }
      resolved.add(new Resolved(code, infoOpt.get()));
    }

    if (resolved.isEmpty()) return Map.of();

    // 2) group by provider (ny, florida, ...)
    Map<String, List<Resolved>> byProvider =
        resolved.stream().collect(Collectors.groupingBy(r -> r.info().provider()));

    // 3) fetch once per provider
    Map<String, ExternalDrawResult> out = new LinkedHashMap<>();

    for (var entry : byProvider.entrySet()) {
      var providerKey = entry.getKey(); // "ny" / "florida" (selon ton yaml)
      List<Resolved> wanted = entry.getValue();

      var provider = findProvider(providerKey);
      if (provider == null) {
        log.debug("uslottery-adapter: no client for providerKey={}", providerKey);
        continue;
      }

      List<LatestDraw> latest;
      try {
        // IMPORTANT: providers devront accepter maxDraws + channelCodes ensuite.
        // Pour l’instant, ça fetch "bulk provider", mais au moins 1 fois.
        latest = provider.fetchLatestDraws();
      } catch (Exception e) {
        log.warn("uslottery-adapter: provider {} failed: {}", provider.provider(), e.toString());
        continue;
      }

      if (CollectionUtils.isEmpty(latest)) continue;

      for (Resolved r : wanted) {
        Optional<LatestDraw> match =
            latest.stream()
                .filter(d -> r.channelCode().equalsIgnoreCase(safe(d.channelCode())))
                .filter(d -> query.drawDateLocal().equals(d.drawDate()))
                .findFirst();

        if (match.isEmpty()) continue;

        LatestDraw d = match.get();
        Instant occurredAt = d.occurredAtUtc() != null ? d.occurredAtUtc().toInstant() : null;

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("provider", provider.provider().name());
        raw.put("external_key", r.info().externalKey());
        raw.put("origin", d.origin());
        raw.put("channel_code", d.channelCode());
        raw.put("draw_date", String.valueOf(d.drawDate()));

        // quality: COMPLETE si taille OK, sinon SUSPECT
        var quality = d.quality();

        out.put(
            r.channelCode(),
            ExternalDrawResult.found(
                "FOUND", toStrings(d.numbers()), toStrings(d.extras()), occurredAt, quality, raw));
      }
    }

    return out;
  }

  private LatestDrawProviderClient findProvider(String providerKeyFromRegistry) {
    // registry.provider() retourne "ny" / "florida" (clé yaml),
    // provider.provider().name() retourne "NY"/"FLORIDA" (enum).
    // On tente les deux match.
    for (var p : providers) {
      if (p.provider().name().equalsIgnoreCase(providerKeyFromRegistry)) return p;
    }
    for (var p : providers) {
      if (p.provider()
          .name()
          .replace("_", "")
          .equalsIgnoreCase(providerKeyFromRegistry.replace("_", ""))) return p;
    }
    // si tu as une mapping propre: providerKey -> enum, tu remplaces ça par un switch.
    return null;
  }

  private List<String> toStrings(DrawMain main) {
    if (main == null || main.ordered() == null) return List.of();
    return main.ordered();
  }

  private List<String> toStrings(DrawExtras extras) {
    if (extras == null || extras.extraNumbers() == null) return List.of();
    return extras.extraNumbers().stream().map(String::valueOf).toList();
  }

  private String safe(String s) {
    return s == null ? "" : s.trim();
  }
}
