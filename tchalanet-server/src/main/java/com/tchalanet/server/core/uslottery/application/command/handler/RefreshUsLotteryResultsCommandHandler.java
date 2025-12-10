package com.tchalanet.server.core.uslottery.application.command.handler;

import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.handler.FetchAndApplyExternalResultCommandHandler;
import com.tchalanet.server.core.draw.application.command.model.FetchAndApplyExternalResultCommand;
import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import com.tchalanet.server.core.uslottery.domain.ports.out.LatestDrawProviderClient;
import com.tchalanet.server.core.uslottery.domain.ports.out.UsLotterySyncStatePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@UseCase
public class RefreshUsLotteryResultsCommandHandler {
    private final List<LatestDrawProviderClient> providers;
    private final UsLotterySyncStatePort syncStatePort;
    private final FetchAndApplyExternalResultCommandHandler fetchAndApplyExternalResultCommandHandler;

    // Placeholder for mapping externalKey + provider -> game.code + draw_channel.code
    // This would typically be done via a dedicated port/service in the draw domain or a mapping
    // config.
    private UUID resolveDrawId(String provider, String externalKey, String channelCode) {
        // In a real system, this would resolve the actual drawId based on provider, externalKey,
        // channelCode, and drawDate
        // For now, return a dummy UUID
        return UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"); // Dummy Draw ID
    }

    @TchTx
    public void refresh() {
        log.info("Starting refresh of all US lottery providers.");

        for (LatestDrawProviderClient client : providers) {
            try {
                List<LatestDraw> latestDraws = client.fetchLatestDraws();
                if (latestDraws == null || latestDraws.isEmpty()) {
                    log.debug("Provider {} returned no new draws.", client.provider());
                    continue;
                }

                for (LatestDraw dto : latestDraws) {
                    if (syncStatePort.shouldFetch(dto)) {
                        syncStatePort.markFetchAttempt(dto);

                        // Resolve the actual Draw ID in the draw domain (placeholder)
                        UUID resolvedDrawId =
                            resolveDrawId(
                                client.provider().name(),
                                dto.externalKey(),
                                dto.channelCode());

                        // Map LatestDraw to ApplyDrawResultCommand (current command has drawId, executedAt)
                        var command = new FetchAndApplyExternalResultCommand(resolvedDrawId, Instant.now());

                        fetchAndApplyExternalResultCommandHandler.handle(command);
                        log.info(
                            "Successfully applied result for draw {} from provider {}",
                            resolvedDrawId,
                            client.provider());

                    } else {
                        log.trace(
                            "Skipping fetch for provider={}, channel={}, date={} (already processed).",
                            client.provider(),
                            dto.channelCode(),
                            dto.drawTimeUtc());
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
