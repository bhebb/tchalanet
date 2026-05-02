package com.tchalanet.server.core.uslottery.infra.external.tx;

import com.tchalanet.server.common.crypto.Hashing;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderClient;
import com.tchalanet.server.core.uslottery.infra.cache.ProviderQueryHash;
import com.tchalanet.server.core.uslottery.infra.cache.UsLotteryProviderRawCache;
import com.tchalanet.server.core.uslottery.infra.config.UsLotteryProperties;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.tx",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class TexasDrawResultsClient implements UsLotteryProviderClient {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.TX;
    private static final String SHAPE = "TX/rss/v2";

    @Qualifier("txLotteryRestClient")
    private final RestClient txLotteryRestClient;

    private final UsLotteryProperties props;
    private final UsLotteryProviderRawCache cache;
    private final TexasDrawResultsMapper mapper;

    @Override
    public UsLotteryProvider provider() {
        return PROVIDER;
    }

    @Override
    public UsLotteryProviderResponse fetch(UsLotteryProviderQuery query) {
        Objects.requireNonNull(query, "query required");

        var cfg = props.getProviders() == null ? null : props.getProviders().get("tx");
        if (cfg == null || !cfg.isEnabled() || StringUtils.isBlank(cfg.getLatestPath())) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var url = joinUrl(cfg.getBaseUrl(), cfg.getLatestPath());

        var queryHash =
            ProviderQueryHash.of(
                PROVIDER.name(),
                query.drawDate(),
                query.drawTime(),
                query.gameCodes().stream().sorted().toList(),
                SHAPE + "|" + url);

        var xml =
            cache.getOrFetch(
                PROVIDER.name(),
                query.drawDate(),
                queryHash,
                () -> fetchXml(url));

        if (StringUtils.isBlank(xml)) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        return mapper.map(xml, Hashing.sha256Hex(xml), url, query);
    }

    private String fetchXml(String url) {
        try {
            return txLotteryRestClient.get().uri(url).retrieve().body(String.class);
        } catch (Exception e) {
            log.warn("tx-client fetch failed url={} err={}", url, e.getMessage(), e);
            return null;
        }
    }

    private static String joinUrl(String base, String path) {
        var b = StringUtils.defaultString(base).trim();
        var p = StringUtils.defaultString(path).trim();

        if (b.endsWith("/") && p.startsWith("/")) {
            return b.substring(0, b.length() - 1) + p;
        }
        if (!b.endsWith("/") && !p.startsWith("/") && !b.isBlank() && !p.isBlank()) {
            return b + "/" + p;
        }
        return b + p;
    }
}
