package com.tchalanet.server.core.uslottery.infra.adapter;

import com.tchalanet.server.core.draw.application.port.out.ExternalDrawResultPort;
import com.tchalanet.server.core.uslottery.application.port.out.LatestDrawProviderClient;
import com.tchalanet.server.core.uslottery.domain.model.DrawExtras;
import com.tchalanet.server.core.uslottery.domain.model.DrawMain;
import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import com.tchalanet.server.core.uslottery.infra.config.UsLotteryGameRegistry;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

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
              "FOUND",
              toStrings(d.numbers()),
              toStrings(d.extras()),
              occurredAt,
              raw);
        }
      } catch (Exception e) {
        log.warn("uslottery-adapter: provider {} failed: {}", provider.provider(), e.toString());
      }
    }

    return ExternalDrawResult.notFound("NOT_FOUND", Map.of("channel_code", channelCode));
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
