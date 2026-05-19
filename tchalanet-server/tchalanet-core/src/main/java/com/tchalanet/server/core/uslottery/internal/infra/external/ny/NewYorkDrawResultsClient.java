package com.tchalanet.server.core.uslottery.internal.infra.external.ny;

import com.tchalanet.server.common.crypto.Hashing;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderClient;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.infra.cache.ProviderQueryHash;
import com.tchalanet.server.core.uslottery.internal.infra.cache.UsLotteryProviderRawCache;
import com.tchalanet.server.core.uslottery.internal.infra.config.UsLotteryProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * NY strategy:
 * <p>
 * 1. Primary source: NY Lottery Drupal API, because it publishes same-day results earlier.
 * 2. Fallback source: NY Open Data, same requested date/slot only.
 * 3. Never fill a requested draw date with a previous provider date.
 */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.ny",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class NewYorkDrawResultsClient implements UsLotteryProviderClient {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.NY;

    private static final String DRUPAL_SHAPE = "NY/drupal-winning-numbers/v2";
    private static final String OPEN_DATA_SHAPE = "NY/open-data/v2";

    private static final Map<String, String> DRUPAL_GAME_NIDS =
        Map.of(
            "NUMBERS", "41",
            "WIN4", "46"
        );

    private final RestClient nyRestClient;
    private final UsLotteryProperties props;
    private final UsLotteryProviderRawCache cache;
    private final NewYorkDrupalMapper drupalMapper;
    private final NewYorkOpenDataMapper openDataMapper;

    public NewYorkDrawResultsClient(
        @Qualifier("nyLotteryRestClient") RestClient nyRestClient,
        UsLotteryProperties props,
        UsLotteryProviderRawCache cache,
        NewYorkDrupalMapper drupalMapper,
        NewYorkOpenDataMapper openDataMapper
    ) {
        this.nyRestClient = Objects.requireNonNull(nyRestClient);
        this.props = Objects.requireNonNull(props);
        this.cache = Objects.requireNonNull(cache);
        this.drupalMapper = Objects.requireNonNull(drupalMapper);
        this.openDataMapper = Objects.requireNonNull(openDataMapper);
    }

    @Override
    public UsLotteryProvider provider() {
        return PROVIDER;
    }

    @Override
    public UsLotteryProviderResponse fetch(UsLotteryProviderQuery query) {
        Objects.requireNonNull(query, "query required");

        var cfg = props.getProviders() == null ? null : props.getProviders().get("ny");
        if (cfg == null
            || !cfg.isEnabled()
            || StringUtils.isBlank(cfg.getBaseUrl())
            || StringUtils.isBlank(cfg.getLatestPath())) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var drupalUrl = joinUrl(cfg.getBaseUrl(), cfg.getLatestPath());
        var drupalRawByGame = fetchDrupalRawByGame(query, drupalUrl);

        if (!drupalRawByGame.isEmpty()) {
            var drupalResponse =
                drupalMapper.map(
                    drupalRawByGame,
                    drupalUrl,
                    query,
                    "PRIMARY_DRUPAL_API");

            if (hasAllExpectedGameCodes(drupalResponse, query)) {
                return drupalResponse;
            }

            log.warn(
                "ny-client drupal_missing_or_incomplete requestedDate={} providerSlotCode={} expectedGameCodes={} returnedGameCodes={} fallback=open-data",
                query.drawDate(),
                query.providerSlotCode(),
                query.externalGameCodes(),
                drupalResponse.results().stream().map(r -> r.externalGameCode()).toList());
        } else {
            log.warn(
                "ny-client drupal_empty requestedDate={} providerSlotCode={} fallback=open-data",
                query.drawDate(),
                query.providerSlotCode());
        }

        var openDataUrl = resolveOpenDataUrl(cfg);
        var openDataBody = fetchOpenData(query, cfg.getAppToken(), openDataUrl);

        if (StringUtils.isBlank(openDataBody)) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var openDataResponse =
            openDataMapper.map(
                openDataBody,
                Hashing.sha256Hex(openDataBody),
                openDataUrl,
                query,
                "DRUPAL_UNAVAILABLE_OR_INCOMPLETE");

        if (!hasAllExpectedGameCodes(openDataResponse, query)) {
            log.warn(
                "ny-client no_result requestedDate={} providerSlotCode={} expectedGameCodes={} openDataReturnedGameCodes={}",
                query.drawDate(),
                query.providerSlotCode(),
                query.externalGameCodes(),
                openDataResponse.results().stream().map(r -> r.externalGameCode()).toList());
        }

        return openDataResponse;
    }

    private LinkedHashMap<String, String> fetchDrupalRawByGame(
        UsLotteryProviderQuery query,
        String drupalUrl
    ) {
        var rawByGame = new LinkedHashMap<String, String>();

        for (String gameCode : query.externalGameCodes()) {
            var normalizedGame = normalizeGameCode(gameCode);
            var nid = DRUPAL_GAME_NIDS.get(normalizedGame);

            if (nid == null) {
                log.warn("ny-client drupal unsupported gameCode={}", gameCode);
                continue;
            }

            var body = fetchDrupalGame(query, drupalUrl, normalizedGame, nid);
            if (StringUtils.isNotBlank(body)) {
                rawByGame.put(normalizedGame, body);
            }
        }

        return rawByGame;
    }

    private String fetchDrupalGame(
        UsLotteryProviderQuery query,
        String drupalUrl,
        String gameCode,
        String nid
    ) {
        var queryHash =
            ProviderQueryHash.of(
                PROVIDER.name(),
                query.drawDate(),
                query.drawTime(),
                List.of(gameCode),
                DRUPAL_SHAPE
                    + "|url=" + drupalUrl
                    + "|game=" + gameCode
                    + "|nid=" + nid
                    + "|providerSlotCode=" + StringUtils.defaultString(query.providerSlotCode()));

        return cache.getOrFetch(
            PROVIDER.name(),
            query.drawDate(),
            queryHash,
            () -> performDrupalFetch(query, drupalUrl, nid));
    }

    private String performDrupalFetch(
        UsLotteryProviderQuery query,
        String drupalUrl,
        String nid
    ) {
        try {
            var body = nyRestClient
                .get()
                .uri(
                    drupalUrl
                        + "?_format=json"
                        + "&nid=" + urlEncode(nid)
                        + "&page=0")
                .retrieve()
                .body(String.class);

            log.info(
                "ny-client drupal fetched nid={} drawDate={} providerSlotCode={} bodySize={} sample={}",
                nid,
                query.drawDate(),
                query.providerSlotCode(),
                body == null ? 0 : body.length(),
                sample(body));

            return body;
        } catch (Exception ex) {
            log.warn(
                "ny-client drupal fetch_failed nid={} drawDate={} providerSlotCode={} err={}",
                nid,
                query.drawDate(),
                query.providerSlotCode(),
                ex.getLocalizedMessage(),
                ex);
            return null;
        }
    }

    private String fetchOpenData(
        UsLotteryProviderQuery query,
        String appToken,
        String openDataUrl
    ) {
        var queryHash =
            ProviderQueryHash.of(
                PROVIDER.name(),
                query.drawDate(),
                query.drawTime(),
                query.externalGameCodes().stream().sorted().toList(),
                OPEN_DATA_SHAPE
                    + "|url=" + openDataUrl
                    + "|providerSlotCode=" + StringUtils.defaultString(query.providerSlotCode()));

        return cache.getOrFetch(
            PROVIDER.name(),
            query.drawDate(),
            queryHash,
            () -> performOpenDataFetch(query, appToken, openDataUrl));
    }

    private String performOpenDataFetch(
        UsLotteryProviderQuery query,
        String appToken,
        String openDataUrl
    ) {
        try {
            var body = nyRestClient
                .get()
                .uri(uriBuilder -> {
                    var builder =
                        uriBuilder
                            .scheme("https")
                            .host("data.ny.gov")
                            .path("/resource/hsys-3def.json")
                            .queryParam("$limit", 20)
                            .queryParam(
                                "$select",
                                "draw_date,midday_daily,evening_daily,midday_win_4,evening_win_4")
                            .queryParam(
                                "$where",
                                "draw_date <= '" + query.drawDate() + "T23:59:59.000'")
                            .queryParam("$order", "draw_date DESC");

                    if (StringUtils.isNotBlank(appToken)) {
                        builder.queryParam("$$app_token", appToken);
                    }

                    return builder.build();
                })
                .retrieve()
                .body(String.class);

            log.info(
                "ny-client open-data fetched drawDate={} providerSlotCode={} bodySize={} sample={}",
                query.drawDate(),
                query.providerSlotCode(),
                body == null ? 0 : body.length(),
                sample(body));

            return body;
        } catch (Exception ex) {
            log.warn(
                "ny-client open-data fetch_failed drawDate={} providerSlotCode={} err={}",
                query.drawDate(),
                query.providerSlotCode(),
                ex.getLocalizedMessage(),
                ex);
            return null;
        }
    }

    private boolean hasAllExpectedGameCodes(
        UsLotteryProviderResponse response,
        UsLotteryProviderQuery query
    ) {
        if (response == null || response.results() == null || response.results().isEmpty()) {
            return false;
        }

        var returned =
            response.results().stream()
                .map(r -> normalizeGameCode(r.externalGameCode()))
                .collect(java.util.stream.Collectors.toSet());

        var expected =
            query.externalGameCodes().stream()
                .filter(Objects::nonNull)
                .map(NewYorkDrawResultsClient::normalizeGameCode)
                .collect(java.util.stream.Collectors.toSet());

        return returned.containsAll(expected);
    }

    private static String resolveOpenDataUrl(UsLotteryProperties.ProviderProperties cfg) {
        // Keep Open Data as an implementation-level fallback for now.
        // The URL can later be externalized with dedicated open-data-* properties.
        if (cfg.getBaseUrl() != null) {
            return cfg.getBaseUrl();
        }
        return "https://data.ny.gov/resource/hsys-3def.json";
    }

    private static String normalizeGameCode(String value) {
        return value == null
            ? ""
            : value.trim().toUpperCase(java.util.Locale.ROOT).replaceAll("\\s+", "");
    }

    private static String sample(String value) {
        return value == null ? "" : value.substring(0, Math.min(300, value.length()));
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(
            StringUtils.defaultString(value),
            StandardCharsets.UTF_8);
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
