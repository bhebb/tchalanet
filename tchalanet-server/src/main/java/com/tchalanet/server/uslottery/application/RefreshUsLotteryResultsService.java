package com.tchalanet.server.uslottery.application;

import com.tchalanet.server.draw.application.command.handler.FetchAndApplyExternalResultUseCase;
import com.tchalanet.server.draw.application.command.model.FetchAndApplyExternalResultCommand;
import com.tchalanet.server.draw.domain.model.DrawSource;
import com.tchalanet.server.uslottery.domain.dto.LatestDrawDto;
import com.tchalanet.server.uslottery.domain.ports.in.RefreshUsLotteryResultsUseCase;
import com.tchalanet.server.uslottery.domain.ports.out.LatestDrawProviderClient;
import com.tchalanet.server.uslottery.domain.ports.out.UsLotterySyncStatePort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshUsLotteryResultsService implements RefreshUsLotteryResultsUseCase {

  private final List<LatestDrawProviderClient> providers;
  private final UsLotterySyncStatePort syncStatePort;
  private final FetchAndApplyExternalResultUseCase fetchAndApplyExternalResultUseCase;

  // Placeholder for mapping externalKey + provider -> game.code + draw_channel.code
  // This would typically be done via a dedicated port/service in the draw domain or a mapping
  // config.
  private UUID resolveDrawId(String provider, String externalKey, String channelCode) {
    // In a real system, this would resolve the actual drawId based on provider, externalKey,
    // channelCode, and drawDate
    // For now, return a dummy UUID
    return UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"); // Dummy Draw ID
  }

  @Override
  @Transactional
  public void refresh() {
    log.info("Starting refresh of all US lottery providers.");

    for (LatestDrawProviderClient client : providers) {
      try {
        List<LatestDrawDto> latestDraws = client.fetchLatestDraws();
        if (latestDraws == null || latestDraws.isEmpty()) {
          log.debug("Provider {} returned no new draws.", client.provider());
          continue;
        }

        for (LatestDrawDto dto : latestDraws) {
          if (syncStatePort.shouldFetch(dto)) {
            syncStatePort.markFetchAttempt(dto);

            // Resolve the actual Draw ID in the draw domain (placeholder)
            UUID resolvedDrawId =
                resolveDrawId(
                    client.provider().name(),
                    dto.externalChannelCode(),
                    dto.scheduledAt().toString());

            // Map LatestDrawDto to ApplyDrawResultCommand
            var command =
                new FetchAndApplyExternalResultCommand(
                    resolvedDrawId,
                    UUID.fromString(
                        "00000000-0000-0000-0000-000000000001"), // Placeholder Tenant ID
                    DrawSource.US_LOTTERY, // Source is US_LOTTERY for external provider
                    Instant.now(),
                    Map.of("resultPayloadJson", dto.resultPayloadJson()));

            fetchAndApplyExternalResultUseCase.handle(
                command); // Call the use case in the draw domain
            log.info(
                "Successfully applied result for draw {} from provider {}",
                resolvedDrawId,
                client.provider());

          } else {
            log.trace(
                "Skipping fetch for provider={}, channel={}, date={} (already processed).",
                client.provider(),
                dto.externalChannelCode(),
                dto.scheduledAt());
          }
        }
      } catch (Exception e) {
        log.warn(
            "Provider {} failed to fetch latest draws: {}", client.provider(), e.getMessage(), e);
      }
    }

    log.info("Finished refresh of all US lottery providers.");
  }
}
