package com.tchalanet.server.core.uslottery.internal.infra.external.nj;

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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * New Jersey — internal JSON, paginated draw-games feed.
 *
 * <pre>
 * /api/v1/draw-games/draws/page?date-from=YYYY-MM-DD&amp;date-to=YYYY-MM-DD
 *   &amp;game-names=Pick+3&amp;status=CLOSED&amp;size=1000&amp;page=0
 * </pre>
 */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.nj",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class NewJerseyDrawResultsClient implements UsLotteryProviderClient {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.NJ;
    private static final String SHAPE = "NJ/draw-games/page/v1";

    private final RestClient rest;
    private final UsLotteryProperties props;
    private final UsLotteryProviderRawCache cache;
    private final NewJerseyDrawResultsMapper mapper;

    public NewJerseyDrawResultsClient(
        @Qualifier("njLotteryRestClient") RestClient rest,
        UsLotteryProperties props,
        UsLotteryProviderRawCache cache,
        NewJerseyDrawResultsMapper mapper) {
        this.rest = Objects.requireNonNull(rest);
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

        var cfg = props.getProviders() == null ? null : props.getProviders().get("nj");
        if (cfg == null || !cfg.isEnabled() || StringUtils.isBlank(cfg.getLatestPath())) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var url = buildUrl(cfg.getBaseUrl(), cfg.getLatestPath(), query);

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

    private String buildUrl(String base, String path, UsLotteryProviderQuery query) {
        // status=CLOSED + epoch-millis bounds (provider-tz midnight of the draw date and the next
        // day); the mapper filters to the exact date + slot.
        var from = query.drawDate().atStartOfDay(query.timezone()).toInstant().toEpochMilli();
        var to = query.drawDate().plusDays(1).atStartOfDay(query.timezone()).toInstant().toEpochMilli();

        var sb = new StringBuilder(joinUrl(base, path));
        sb.append("?status=CLOSED")
            .append("&size=2000")
            .append("&page=0")
            .append("&date-from=").append(from)
            .append("&date-to=").append(to);

        for (var gameName : gameNames(query.externalGameCodes())) {
            sb.append("&game-names=").append(URLEncoder.encode(gameName, StandardCharsets.UTF_8));
        }

        return sb.toString();
    }

    /** Maps internal external-game-codes (PICK3/PICK4) to NJ provider game names. */
    private static Set<String> gameNames(Set<String> codes) {
        var names = new LinkedHashSet<String>();
        for (var code : codes) {
            switch (StringUtils.upperCase(code)) {
                case "PICK3", "CASH3" -> names.add("Pick 3");
                case "PICK4", "CASH4" -> names.add("Pick 4");
                default -> { /* unknown code: skip, let provider return its default set */ }
            }
        }
        return names;
    }

    private String fetchBody(String url) {
        try {
            return rest.get().uri(url).retrieve().body(String.class);
        } catch (Exception e) {
            log.warn("nj-client fetch failed url={} err={}", url, e.getMessage(), e);
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
