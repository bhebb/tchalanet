package com.tchalanet.server.core.uslottery.internal.infra.external.ny;

import com.tchalanet.server.common.util.Hashing;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderClient;
import com.tchalanet.server.core.uslottery.internal.infra.cache.ProviderQueryHash;
import com.tchalanet.server.core.uslottery.internal.infra.cache.UsLotteryProviderRawCache;
import com.tchalanet.server.core.uslottery.internal.infra.config.UsLotteryProperties;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.ny",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class NewYorkDrawResultsClient implements UsLotteryProviderClient {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.NY;
    private static final String SHAPE = "NY/soql/v3";

    private final RestClient nyRestClient;
    private final UsLotteryProperties props;
    private final UsLotteryProviderRawCache cache;
    private final NewYorkDrawResultsMapper mapper;

    public NewYorkDrawResultsClient(
        @Qualifier("nyLotteryRestClient") RestClient nyRestClient,
        UsLotteryProperties props,
        UsLotteryProviderRawCache cache,
        NewYorkDrawResultsMapper mapper) {
        this.nyRestClient = Objects.requireNonNull(nyRestClient);
        this.props = Objects.requireNonNull(props);
        this.cache = Objects.requireNonNull(cache);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public UsLotteryProvider provider() {
        return PROVIDER;
    }

    @Override
    public UsLotteryProviderResponse fetch(UsLotteryProviderQuery query) {
        Objects.requireNonNull(query, "query required");

        var cfg = props.getProviders() == null ? null : props.getProviders().get("ny");
        if (cfg == null || !cfg.isEnabled()) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var queryHash =
            ProviderQueryHash.of(
                PROVIDER.name(),
                query.drawDate(),
                query.drawTime(),
                query.externalGameCodes().stream().sorted().toList(),
                SHAPE);

        var body =
            cache.getOrFetch(
                PROVIDER.name(),
                query.drawDate(),
                queryHash,
                () -> performFetch(query, cfg.getAppToken()));

        if (StringUtils.isBlank(body)) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        return mapper.map(body, Hashing.sha256Hex(body), query);
    }

    private String performFetch(UsLotteryProviderQuery query, String appToken) {
        try {
            return nyRestClient
                .get()
                .uri(
                    uriBuilder -> {
                        var builder =
                            uriBuilder
                                .queryParam("$limit", 20)
                                .queryParam("$select",
                                    "draw_date,midday_daily,evening_daily,midday_win_4,evening_win_4")
                                .queryParam("$where", "draw_date <= '" + query.drawDate() + "T23:59:59.000'")
                                .queryParam("$order", "draw_date DESC");

                        if (StringUtils.isNotBlank(appToken)) {
                            builder.queryParam("app_token", appToken);
                        }

                        return builder.build();
                    })
                .retrieve()
                .body(String.class);
        } catch (Exception ex) {
            log.warn("Failed to fetch NY lottery data: {}", ex.getLocalizedMessage(), ex);
            return null;
        }
    }
}
