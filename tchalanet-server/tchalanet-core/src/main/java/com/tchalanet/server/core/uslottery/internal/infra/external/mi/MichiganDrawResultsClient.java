package com.tchalanet.server.core.uslottery.internal.infra.external.mi;

import com.tchalanet.server.common.crypto.Hashing;
import com.tchalanet.server.common.json.utils.JsonbUtils;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderClient;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.internal.infra.cache.ProviderQueryHash;
import com.tchalanet.server.core.uslottery.internal.infra.cache.UsLotteryProviderRawCache;
import com.tchalanet.server.core.uslottery.internal.infra.config.UsLotteryProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Michigan — internal GraphQL endpoint.
 *
 * <pre>POST /api  { operationName, variables{ logicalGameIdentifier, drawDate }, query }</pre>
 *
 * <p>logicalGameIdentifier = DAILY_3 / DAILY_4. drawDate = {@code yyyy-MM-ddT04:00:00.000Z} for the
 * local draw date. The GraphQL query string is a best-effort default; adjust after a real curl.
 */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.mi",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class MichiganDrawResultsClient implements UsLotteryProviderClient {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.MI;
    private static final String SHAPE = "MI/graphql/v1";

    private final RestClient rest;
    private final UsLotteryProperties props;
    private final UsLotteryProviderRawCache cache;
    private final MichiganDrawResultsMapper mapper;
    private final JsonbUtils json;

    public MichiganDrawResultsClient(
        @Qualifier("miLotteryRestClient") RestClient rest,
        UsLotteryProperties props,
        UsLotteryProviderRawCache cache,
        MichiganDrawResultsMapper mapper,
        JsonbUtils json) {
        this.rest = Objects.requireNonNull(rest);
        this.props = Objects.requireNonNull(props);
        this.cache = Objects.requireNonNull(cache);
        this.mapper = Objects.requireNonNull(mapper);
        this.json = Objects.requireNonNull(json);
    }

    @Override
    public UsLotteryProvider provider() {
        return PROVIDER;
    }

    @Override
    public UsLotteryProviderResponse fetch(UsLotteryProviderQuery query) {
        Objects.requireNonNull(query, "query required");

        var cfg = props.getProviders() == null ? null : props.getProviders().get("mi");
        if (cfg == null || !cfg.isEnabled() || StringUtils.isBlank(cfg.getLatestPath())) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var url = joinUrl(cfg.getBaseUrl(), cfg.getLatestPath());
        var drawDate = query.drawDate() + "T04:00:00.000Z";
        var merged = new ArrayList<UsLotteryProviderResult>();

        for (var code : query.externalGameCodes()) {
            var game = MiGame.resolve(code);
            if (game.isEmpty()) {
                continue;
            }

            var requestBody = buildGraphQlBody(game.get(), drawDate);

            var queryHash =
                ProviderQueryHash.of(
                    PROVIDER.name(),
                    query.drawDate(),
                    query.drawTime(),
                    List.of(game.get().outputCode()),
                    SHAPE + "|" + url + "|op=" + game.get().operation()
                        + "|providerSlotCode=" + StringUtils.defaultString(query.providerSlotCode()));

            var body = cache.getOrFetch(PROVIDER.name(), query.drawDate(), queryHash, () -> post(url, requestBody));
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

    private String buildGraphQlBody(MiGame game, String drawDate) {
        var variables = new LinkedHashMap<String, Object>();
        variables.put("logicalGameIdentifier", game.logicalGameIdentifier());
        variables.put("drawDate", drawDate);

        // The endpoint exposes winningNumbers as a root query field taking logicalGameIdentifier +
        // drawDate; the response surfaces it at data.winningNumbers (see mapper for the full shape).
        var graphQlQuery =
            "query WinningNumbers($logicalGameIdentifier: String!, $drawDate: String!) { "
                + "winningNumbers(logicalGameIdentifier: $logicalGameIdentifier, drawDate: $drawDate) { "
                + "drawNumbersMid drawNumbersEve resultsPending isBonusDrawMid isBonusDrawEve } }";

        var payload = new LinkedHashMap<String, Object>();
        payload.put("operationName", "WinningNumbers");
        payload.put("variables", variables);
        payload.put("query", graphQlQuery);

        return json.toJson(payload);
    }

    private String post(String url, String body) {
        try {
            return rest.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            log.warn("mi-client post failed url={} err={}", url, e.getMessage(), e);
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

    enum MiGame {
        DAILY3("PICK3", "daily3Data", "DAILY_3", 3),
        DAILY4("PICK4", "daily4Data", "DAILY_4", 4);

        private final String outputCode;
        private final String operation;
        private final String logicalGameIdentifier;
        private final int expectedSize;

        MiGame(String outputCode, String operation, String logicalGameIdentifier, int expectedSize) {
            this.outputCode = outputCode;
            this.operation = operation;
            this.logicalGameIdentifier = logicalGameIdentifier;
            this.expectedSize = expectedSize;
        }

        String outputCode() {
            return outputCode;
        }

        String operation() {
            return operation;
        }

        String logicalGameIdentifier() {
            return logicalGameIdentifier;
        }

        int expectedSize() {
            return expectedSize;
        }

        static Optional<MiGame> resolve(String rawCode) {
            if (rawCode == null) {
                return Optional.empty();
            }
            var n = rawCode.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s_\\-]+", "");
            return switch (n) {
                case "PICK3", "CASH3", "DAILY3" -> Optional.of(DAILY3);
                case "PICK4", "CASH4", "DAILY4" -> Optional.of(DAILY4);
                default -> Optional.empty();
            };
        }
    }
}
