package com.tchalanet.server.core.uslottery.internal.infra.adapter;

import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalResultFetchBundle;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalResultFetchQuery;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalResultItem;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalResultsFetchPort;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalSourceFlags;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.internal.infra.registry.ProviderClientRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class UsLotteryExternalResultsFetchAdapter implements ExternalResultsFetchPort {

    private final ProviderClientRegistry registry;

    @Override
    public ExternalResultFetchBundle fetchProviderResults(ExternalResultFetchQuery query) {
        var provider = resolveProvider(query.provider());

        if (provider == null) {
            log.warn(
                "uslottery fetch skipped unknown provider={} drawDate={} drawTime={} providerSlotCode={}",
                query.provider(),
                query.drawDate(),
                query.drawTime(),
                query.providerSlotCode());
            return ExternalResultFetchBundle.empty(query.provider(), query);
        }

        if (query.gameCodes() == null || query.gameCodes().isEmpty()) {
            log.info(
                "uslottery fetch skipped provider={} drawDate={} drawTime={} providerSlotCode={} reason=no_active_game_codes",
                provider,
                query.drawDate(),
                query.drawTime(),
                query.providerSlotCode());
            return ExternalResultFetchBundle.empty(provider.name(), query);
        }

        var client = registry.find(provider);
        if (client.isEmpty()) {
            log.warn(
                "uslottery fetch skipped provider={} drawDate={} drawTime={} providerSlotCode={} reason=no_client_registered",
                provider,
                query.drawDate(),
                query.drawTime(),
                query.providerSlotCode());
            return ExternalResultFetchBundle.empty(provider.name(), query);
        }

        try {
            var response = client.get().fetch(
                new UsLotteryProviderQuery(
                    query.drawDate(),
                    query.drawTime(),
                    query.timezone(),
                    query.gameCodes(),
                    query.providerSlotCode(),
                    query.force(),
                    query.includeRaw(),
                    query.requestedAt()));

            log.info(
                "uslottery fetch completed provider={} drawDate={} drawTime={} providerSlotCode={} requestedGames={} resultCount={}",
                provider,
                query.drawDate(),
                query.drawTime(),
                query.providerSlotCode(),
                query.gameCodes(),
                response.results().size());

            return toExternalBundle(response);
        } catch (Exception ex) {
            log.warn(
                "uslottery fetch failed provider={} drawDate={} drawTime={} providerSlotCode={} requestedGames={} err={}",
                provider,
                query.drawDate(),
                query.drawTime(),
                query.providerSlotCode(),
                query.gameCodes(),
                ex.getMessage(),
                ex);
            return ExternalResultFetchBundle.empty(provider.name(), query);
        }
    }

    private static UsLotteryProvider resolveProvider(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UsLotteryProvider.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ExternalResultFetchBundle toExternalBundle(UsLotteryProviderResponse response) {
        return new ExternalResultFetchBundle(
            response.provider().name(),
            response.drawDate(),
            response.drawTime(),
            response.timezone(),
            response.results().stream().map(this::toExternalItem).toList(),
            response.rawPayload());
    }

    private ExternalResultItem toExternalItem(UsLotteryProviderResult r) {
        return new ExternalResultItem(
            r.externalGameCode(),
            r.main(),
            r.extras(),
            r.quality(),
            new ExternalSourceFlags(
                r.sourceFlags().origin(),
                r.sourceFlags().sourceHash(),
                r.sourceFlags().url(),
                r.sourceFlags().metadata()),
            r.occurredAt(),
            r.rawPayload());
    }
}
