package com.tchalanet.server.core.uslottery.internal.infra.external.ca;

import com.tchalanet.server.common.crypto.Hashing;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderClient;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.internal.infra.cache.ProviderQueryHash;
import com.tchalanet.server.core.uslottery.internal.infra.cache.UsLotteryProviderRawCache;
import com.tchalanet.server.core.uslottery.internal.infra.config.UsLotteryProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * California — internal JSON, per-game past-draw results.
 *
 * <pre>/api/DrawGameApi/DrawGamePastDrawResults/{gameId}/1/20</pre>
 *
 * <p>DAILY3 -&gt; gameId 9 (confirmed). DAILY4 -&gt; gameId 10 (sequential, needs verification
 * via {@code DrawGamePastDrawResults/10/1/20} — if wrong the fetch returns empty, no crash).
 */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.ca",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class CaliforniaDrawResultsClient implements UsLotteryProviderClient {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.CA;
    private static final String SHAPE = "CA/past-draw/v1";

    private final RestClient rest;
    private final UsLotteryProperties props;
    private final UsLotteryProviderRawCache cache;
    private final CaliforniaDrawResultsMapper mapper;

    public CaliforniaDrawResultsClient(
        @Qualifier("caLotteryRestClient") RestClient rest,
        UsLotteryProperties props,
        UsLotteryProviderRawCache cache,
        CaliforniaDrawResultsMapper mapper) {
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

        var cfg = props.getProviders() == null ? null : props.getProviders().get("ca");
        if (cfg == null || !cfg.isEnabled() || StringUtils.isBlank(cfg.getLatestPath())) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var merged = new ArrayList<UsLotteryProviderResult>();

        for (var code : query.externalGameCodes()) {
            var game = CaGame.resolve(code);
            if (game.isEmpty()) {
                log.debug("ca-client skipped unmapped/unenabled gameCode={}", code);
                continue;
            }

            var url = joinUrl(cfg.getBaseUrl(), cfg.getLatestPath()) + "/" + game.get().gameId() + "/1/20";

            var queryHash =
                ProviderQueryHash.of(
                    PROVIDER.name(),
                    query.drawDate(),
                    query.drawTime(),
                    List.of(game.get().outputCode()),
                    SHAPE + "|" + url + "|providerSlotCode=" + StringUtils.defaultString(query.providerSlotCode()));

            var body = cache.getOrFetch(PROVIDER.name(), query.drawDate(), queryHash, () -> fetchBody(url));
            if (StringUtils.isBlank(body)) {
                continue;
            }

            var response = mapper.map(body, game.get(), Hashing.sha256Hex(body), url, query);
            merged.addAll(response.results());
        }

        return new UsLotteryProviderResponse(
            PROVIDER,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            List.copyOf(merged),
            null);
    }

    private String fetchBody(String url) {
        try {
            return rest.get().uri(url).retrieve().body(String.class);
        } catch (Exception e) {
            log.warn("ca-client fetch failed url={} err={}", url, e.getMessage(), e);
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

    /** Maps internal external-game-codes to a CA gameId. */
    enum CaGame {
        DAILY3("DAILY3", 9, 3),
        /** gameId=14 confirmed via DrawGamePastDrawResults/14/1/20 — once per day (evening only). */
        DAILY4("DAILY4", 14, 4);

        private final String outputCode;
        private final int gameId;
        private final int expectedSize;

        CaGame(String outputCode, int gameId, int expectedSize) {
            this.outputCode = outputCode;
            this.gameId = gameId;
            this.expectedSize = expectedSize;
        }

        String outputCode() {
            return outputCode;
        }

        int gameId() {
            return gameId;
        }

        int expectedSize() {
            return expectedSize;
        }

        static Optional<CaGame> resolve(String rawCode) {
            if (rawCode == null) {
                return Optional.empty();
            }
            var n = rawCode.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s_\\-]+", "");
            return switch (n) {
                case "DAILY3", "PICK3", "CASH3" -> Optional.of(DAILY3);
                case "DAILY4", "PICK4", "CASH4" -> Optional.of(DAILY4);
                default -> Optional.empty();
            };
        }
    }
}
