package com.tchalanet.server.core.uslottery.internal.infra.external.oh;

import com.tchalanet.server.common.crypto.Hashing;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderClient;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.internal.infra.cache.ProviderQueryHash;
import com.tchalanet.server.core.uslottery.internal.infra.cache.UsLotteryProviderRawCache;
import com.tchalanet.server.core.uslottery.internal.infra.config.UsLotteryProperties;
import com.tchalanet.server.core.uslottery.internal.infra.external.oh.auth.OhioTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Ohio — internal JSON with bearer token.
 *
 * <pre>/1.0/Games/DrawGames/{pick3|pick4|pick5}/SearchDrawDateRange?sinceDate=MM-dd-yyyy&amp;toDate=MM-dd-yyyy</pre>
 *
 * <p>If the bearer token is blank/expired the client returns empty and warns; it never fails the
 * whole fetch. The token is applied as an {@code Authorization} header by the provider RestClient.
 */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.oh",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class OhioDrawResultsClient implements UsLotteryProviderClient {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.OH;
    private static final String SHAPE = "OH/search-range/v1";
    private static final DateTimeFormatter MDY = DateTimeFormatter.ofPattern("MM-dd-uuuu", Locale.US);

    private final RestClient rest;
    private final OhioTokenProvider ohioTokenProvider;
    private final UsLotteryProperties props;
    private final UsLotteryProviderRawCache cache;
    private final OhioDrawResultsMapper mapper;

    public OhioDrawResultsClient(
        @Qualifier("ohLotteryRestClient") RestClient rest,
        OhioTokenProvider ohioTokenProvider,
        UsLotteryProperties props,
        UsLotteryProviderRawCache cache,
        OhioDrawResultsMapper mapper) {
        this.rest = Objects.requireNonNull(rest);
        this.ohioTokenProvider = Objects.requireNonNull(ohioTokenProvider);
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

        var cfg = props.getProviders() == null ? null : props.getProviders().get("oh");
        if (cfg == null || !cfg.isEnabled() || StringUtils.isBlank(cfg.getLatestPath())) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }
        var token = ohioTokenProvider.bearerToken();

        if (token.isEmpty()) {
            log.warn("oh-client skipped drawDate={} reason=missing_bearer_token", query.drawDate());
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var date = query.drawDate().format(MDY);
        var merged = new ArrayList<UsLotteryProviderResult>();

        for (var code : query.externalGameCodes()) {
            var game = OhGame.resolve(code);
            if (game.isEmpty()) {
                continue;
            }

            var url = joinUrl(cfg.getBaseUrl(), cfg.getLatestPath()) + "/" + game.get().pathSegment()
                + "/SearchDrawDateRange?sinceDate=" + date + "&toDate=" + date;

            var queryHash =
                ProviderQueryHash.of(
                    PROVIDER.name(),
                    query.drawDate(),
                    query.drawTime(),
                    List.of(game.get().outputCode()),
                    SHAPE + "|" + url + "|providerSlotCode=" + StringUtils.defaultString(query.providerSlotCode()));

            var body = cache.getOrFetch(
                PROVIDER.name(),
                query.drawDate(),
                queryHash,
                () -> fetchBody(url, token.get())
            );
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

    private String fetchBody(String url, String bearerToken) {
        try {
            return rest.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            log.warn("oh-client fetch failed url={} err={}", url, e.getMessage(), e);
        }
        return null;
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

    enum OhGame {
        PICK3("PICK3", "pick3", 3),
        PICK4("PICK4", "pick4", 4),
        PICK5("PICK5", "pick5", 5);

        private final String outputCode;
        private final String pathSegment;
        private final int expectedSize;

        OhGame(String outputCode, String pathSegment, int expectedSize) {
            this.outputCode = outputCode;
            this.pathSegment = pathSegment;
            this.expectedSize = expectedSize;
        }

        String outputCode() {
            return outputCode;
        }

        String pathSegment() {
            return pathSegment;
        }

        int expectedSize() {
            return expectedSize;
        }

        static Optional<OhGame> resolve(String rawCode) {
            if (rawCode == null) {
                return Optional.empty();
            }
            var n = rawCode.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s_\\-]+", "");
            return switch (n) {
                case "PICK3", "CASH3", "DAILY3" -> Optional.of(PICK3);
                case "PICK4", "CASH4", "DAILY4" -> Optional.of(PICK4);
                case "PICK5", "CASH5", "DAILY5" -> Optional.of(PICK5);
                default -> Optional.empty();
            };
        }
    }
}
