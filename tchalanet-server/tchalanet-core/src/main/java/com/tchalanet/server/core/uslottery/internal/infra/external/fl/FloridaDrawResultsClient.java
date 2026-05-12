package com.tchalanet.server.core.uslottery.internal.infra.external.fl;

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
    prefix = "tch.us-lottery.providers.fl",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class FloridaDrawResultsClient implements UsLotteryProviderClient {

    private static final String SHAPE = "FL/latest/v2";

    private final RestClient rest;
    private final UsLotteryProperties props;
    private final UsLotteryProviderRawCache cache;
    private final FloridaDrawResultsMapper mapper;

    public FloridaDrawResultsClient(
        @Qualifier("floridaLotteryRestClient") RestClient rest,
        UsLotteryProperties props,
        UsLotteryProviderRawCache cache,
        FloridaDrawResultsMapper mapper) {
        this.rest = Objects.requireNonNull(rest);
        this.props = Objects.requireNonNull(props);
        this.cache = Objects.requireNonNull(cache);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public UsLotteryProvider provider() {
        return UsLotteryProvider.FL;
    }

    @Override
    public UsLotteryProviderResponse fetch(UsLotteryProviderQuery query){
        Objects.requireNonNull(query, "query required");

        var providers = props.getProviders();
        var cfg = providers != null ? providers.get("fl") : null;

        if (cfg == null || !cfg.isEnabled() || StringUtils.isBlank(cfg.getLatestPath())) {
            return UsLotteryProviderResponse.empty(provider(), query);
        }

        var fetchUrl = joinUrl(cfg.getBaseUrl(), cfg.getLatestPath());
        var queryHash = buildQueryHash(query, fetchUrl);

        var responseBody =
            cache.getOrFetch(
                provider().name(),
                query.drawDate(),
                queryHash,
                () -> performFetch(fetchUrl));

        if (StringUtils.isBlank(responseBody)) {
            return UsLotteryProviderResponse.empty(provider(), query);
        }

        return mapper.map(
            responseBody,
            Hashing.sha256Hex(responseBody),
            fetchUrl,
            query);
    }

    private String buildQueryHash(UsLotteryProviderQuery query, String url) {
        return ProviderQueryHash.of(
            provider().name(),
            query.drawDate(),
            query.drawTime(),
            query.externalGameCodes().stream().sorted().toList(),
            SHAPE + "|" + url);
    }

    private String performFetch(String url) {
        try {
            return rest.get().uri(url).retrieve().body(String.class);
        } catch (Exception ex) {
            log.warn("Failed to fetch FL lottery data from {}: {}", url, ex.getLocalizedMessage(), ex);
            return null;
        }
    }

    private static String joinUrl(String base, String path) {
        var baseUrl = StringUtils.isBlank(base) ? "" : base.trim();
        var pathComponent = StringUtils.isBlank(path) ? "" : path.trim();

        if (baseUrl.endsWith("/") && pathComponent.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + pathComponent;
        }
        if (!baseUrl.endsWith("/") && !pathComponent.startsWith("/") && !baseUrl.isBlank() && !pathComponent.isBlank()) {
            return baseUrl + "/" + pathComponent;
        }
        return baseUrl + pathComponent;
    }
}
