package com.tchalanet.server.core.uslottery.internal.infra.external.mn;

import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderClient;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Minnesota — Manual provider for now.
 * Only Pick3 is supported.
 */
@Component
@Slf4j
public class MinnesotaDrawResultsClient implements UsLotteryProviderClient {

    private final org.springframework.web.client.RestClient rest;

    public MinnesotaDrawResultsClient(@org.springframework.beans.factory.annotation.Qualifier("mnLotteryRestClient") org.springframework.web.client.RestClient rest) {
        this.rest = rest;
    }

    @Override
    public UsLotteryProvider provider() {
        return UsLotteryProvider.MN;
    }

    @Override
    public UsLotteryProviderResponse fetch(UsLotteryProviderQuery query) {
        log.debug("MN lottery fetch called (manual mode) - returning empty results");
        return UsLotteryProviderResponse.empty(provider(), query);
    }
}
