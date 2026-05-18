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
        var provider = UsLotteryProvider.valueOf(query.provider().trim().toUpperCase(Locale.ROOT));
        log.info("fetching external results provider={} drawDate={} drawTime={} timezone={} gameCodes={} providerSlotCode={} force={} includeRaw={}",
            provider,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            query.gameCodes(),
            query.providerSlotCode(),
            query.force(),
            query.includeRaw());
        var client = registry.get(provider);

        var response =
            client.fetch(
                new UsLotteryProviderQuery(
                    query.drawDate(),
                    query.drawTime(),
                    query.timezone(),
                    query.gameCodes(),
                    query.providerSlotCode(),
                    query.force(),
                    query.includeRaw(),
                    query.requestedAt()));
        log.info("fetched external results provider={} drawDate={} drawTime={} timezone={} gameCodes={} providerSlotCode={}, response={}", provider, query.drawDate(), query.drawTime(), query.timezone(), query.gameCodes(),
            query.providerSlotCode(), response.results().isEmpty() ?  "no results": "results");
        return toExternalBundle(response);
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
