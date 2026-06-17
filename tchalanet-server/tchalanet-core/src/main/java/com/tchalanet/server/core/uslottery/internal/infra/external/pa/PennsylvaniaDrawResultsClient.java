package com.tchalanet.server.core.uslottery.internal.infra.external.pa;

import com.tchalanet.server.common.crypto.Hashing;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderClient;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.infra.cache.ProviderQueryHash;
import com.tchalanet.server.core.uslottery.internal.infra.cache.UsLotteryProviderRawCache;
import com.tchalanet.server.core.uslottery.internal.infra.config.UsLotteryProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Pennsylvania — internal JSON feed. One call returns every game for the day, each top-level entry
 * carrying its evening counterpart under {@code RelatedEveningDrawing}.
 *
 * <pre>/Custom/feeds/DrawingsData.aspx?game=pick%203</pre>
 *
 * <p>The {@code game} query parameter does not actually filter the payload (all games are returned),
 * but it is sent for parity with the site and kept stable for cache keys.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.pa",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class PennsylvaniaDrawResultsClient implements UsLotteryProviderClient {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.PA;
    private static final String SHAPE = "PA/drawings-data/json/v1";

    @Qualifier("paLotteryRestClient")
    private final RestClient paLotteryRestClient;

    private final UsLotteryProperties props;
    private final UsLotteryProviderRawCache cache;
    private final PennsylvaniaDrawResultsMapper mapper;

    @Override
    public UsLotteryProvider provider() {
        return PROVIDER;
    }

    @Override
    public UsLotteryProviderResponse fetch(UsLotteryProviderQuery query) {
        Objects.requireNonNull(query, "query required");

        var cfg = props.getProviders() == null ? null : props.getProviders().get("pa");
        if (cfg == null || !cfg.isEnabled() || StringUtils.isBlank(cfg.getLatestPath())) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var url = joinUrl(cfg.getBaseUrl(), cfg.getLatestPath())
            + "?game=" + URLEncoder.encode("pick 3", StandardCharsets.UTF_8);

        var queryHash =
            ProviderQueryHash.of(
                PROVIDER.name(),
                query.drawDate(),
                query.drawTime(),
                query.externalGameCodes().stream().sorted().toList(),
                SHAPE + "|" + url + "|providerSlotCode=" + StringUtils.defaultString(query.providerSlotCode()));

        var body = cache.getOrFetch(PROVIDER.name(), query.drawDate(), queryHash, () -> fetchBody(url));

        if (StringUtils.isBlank(body)) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        return mapper.map(body, Hashing.sha256Hex(body), url, query);
    }

    private String fetchBody(String url) {
        try {
            return paLotteryRestClient.get().uri(url).retrieve().body(String.class);
        } catch (Exception e) {
            log.warn("pa-client fetch failed url={} err={}", url, e.getMessage(), e);
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
