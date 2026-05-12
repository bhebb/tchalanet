package com.tchalanet.server.core.uslottery.internal.infra.external.ga;

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
    prefix = "tch.us-lottery.providers.ga",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class GeorgiaDrawResultsClient implements UsLotteryProviderClient {

    private static final String SHAPE = "GA/latest/v2";

    private final RestClient rest;
    private final UsLotteryProperties props;
    private final UsLotteryProviderRawCache cache;
    private final GeorgiaDrawResultsMapper mapper;

    public GeorgiaDrawResultsClient(
        @Qualifier("gaLotteryRestClient") RestClient rest,
        UsLotteryProperties props,
        UsLotteryProviderRawCache cache,
        GeorgiaDrawResultsMapper mapper) {
        this.rest = Objects.requireNonNull(rest);
        this.props = Objects.requireNonNull(props);
        this.cache = Objects.requireNonNull(cache);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public UsLotteryProvider provider() {
        return UsLotteryProvider.GA;
    }

    @Override
    public UsLotteryProviderResponse fetch(UsLotteryProviderQuery query) {
        Objects.requireNonNull(query, "query required");

        var providers = props.getProviders();
        var cfg = providers != null ? providers.get("ga") : null;

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
            log.warn("Failed to fetch GA lottery data from {}: {}", url, ex.getLocalizedMessage(), ex);
            return null;
        }
    }

    private static String joinUrl(String base, String path) {
        var b = StringUtils.isBlank(base) ? "" : base.trim();
        var p = StringUtils.isBlank(path) ? "" : path.trim();

        if (b.endsWith("/") && p.startsWith("/")) {
            return b.substring(0, b.length() - 1) + p;
        }
        if (!b.endsWith("/") && !p.startsWith("/") && !b.isBlank() && !p.isBlank()) {
            return b + "/" + p;
        }
        return b + p;
    }
}
