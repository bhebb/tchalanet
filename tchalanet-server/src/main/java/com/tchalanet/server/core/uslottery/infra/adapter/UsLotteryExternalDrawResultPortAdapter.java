package com.tchalanet.server.core.uslottery.infra.adapter;

import com.tchalanet.server.core.draw.application.port.out.ExternalDrawResultPort;
import com.tchalanet.server.core.draw.domain.model.DrawSource;
import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import com.tchalanet.server.core.uslottery.domain.ports.out.LatestDrawProviderClient;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Adapter that implements ExternalDrawResultPort by delegating to available LatestDrawProviderClient
 * providers and mapping their LatestDraw to ExternalDrawResultPort.ExternalDrawResult.
 */
@Component
@Primary
@Slf4j
public class UsLotteryExternalDrawResultPortAdapter implements ExternalDrawResultPort {

  private final List<LatestDrawProviderClient> providers;

  public UsLotteryExternalDrawResultPortAdapter(List<LatestDrawProviderClient> providers) {
    this.providers = providers;
  }

  @Override
  public ExternalDrawResult fetchExternalResult(DrawExternalQuery query) {
    // naive implementation: iterate providers and fetch latest draws, find the best match
    for (LatestDrawProviderClient provider : providers) {
      try {
        List<LatestDraw> latest = provider.fetchLatestDraws();
        if (latest == null || latest.isEmpty()) continue;

        Optional<LatestDraw> match = latest.stream().filter(d -> matches(query, d)).findFirst();
        if (match.isPresent()) {
          LatestDraw d = match.get();
          var occurredAt = d.drawTimeUtc().toInstant();
          var numbers = d.numbersRaw();
          // ExternalDrawResult expects LocalDate drawDate and Instant occurredAt; numbersExtra left empty
          return new ExternalDrawResult(
              d.channelCode(), d.drawDate(), numbers, List.of(), occurredAt, d.origin());
        }
      } catch (Exception e) {
        log.warn("uslottery-adapter: provider {} failed: {}", provider.provider(), e.toString());
      }
    }
    // if not found, return an empty ExternalDrawResult (status INVALID or null fields)
    log.debug("No external result found matching query {}", query);
    return new ExternalDrawResult("", null, List.of(), List.of(), null, "");
  }

  private boolean matches(DrawExternalQuery q, LatestDraw d) {
    // only support US_LOTTERY source for now
    if (q.source() != DrawSource.US_LOTTERY) {
      return false;
    }

    // match dates: compare LocalDate equality
    var drawDateZdt = q.drawDate();
    if (drawDateZdt == null) return true; // if not provided match anything

    var candidate = d.drawTimeUtc();
    if (candidate == null) return false;
    // compare dates ignoring timezones
    return candidate.toLocalDate().equals(drawDateZdt.toLocalDate());
  }
}
