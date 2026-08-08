package com.tchalanet.server.core.uslottery.internal.infra.external.mo;

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
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Missouri Lottery official HTML pages. The site serves Pick 3 and Pick 4 results from separate
 * form-backed endpoints and accepts {@code ddMonth}/{@code ddYear} form parameters.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.mo",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class MissouriDrawResultsClient implements UsLotteryProviderClient {

  private static final UsLotteryProvider PROVIDER = UsLotteryProvider.MO;
  private static final String SHAPE = "MO/winning-numbers/html/v1";

  @Qualifier("moLotteryRestClient")
  private final RestClient moLotteryRestClient;

  private final UsLotteryProperties props;
  private final UsLotteryProviderRawCache cache;
  private final MissouriDrawResultsMapper mapper;

  @Override
  public UsLotteryProvider provider() {
    return PROVIDER;
  }

  @Override
  public UsLotteryProviderResponse fetch(UsLotteryProviderQuery query) {
    Objects.requireNonNull(query, "query required");

    var cfg = props.getProviders() == null ? null : props.getProviders().get("mo");
    if (cfg == null || !cfg.isEnabled()) {
      return UsLotteryProviderResponse.empty(PROVIDER, query);
    }

    var results = new ArrayList<UsLotteryProviderResult>();
    var raw = new StringBuilder();

    fetchGame(cfg, "PICK3", cfg.getLatestPath(), query, results, raw);
    fetchGame(cfg, "PICK4", cfg.getFallbackPath(), query, results, raw);

    return new UsLotteryProviderResponse(
        PROVIDER,
        query.drawDate(),
        query.drawTime(),
        query.timezone(),
        List.copyOf(results),
        query.includeRaw() ? raw.toString() : null);
  }

  private void fetchGame(
      UsLotteryProperties.ProviderProperties cfg,
      String gameCode,
      String path,
      UsLotteryProviderQuery query,
      List<UsLotteryProviderResult> results,
      StringBuilder raw) {

    if (!query.externalGameCodes().contains(gameCode) || StringUtils.isBlank(path)) {
      return;
    }

    var url = joinUrl(cfg.getBaseUrl(), path);
    var form =
        "ddMonth=" + query.drawDate().getMonthValue() + "&ddYear=" + query.drawDate().getYear();
    var queryHash =
        ProviderQueryHash.of(
            PROVIDER.name(),
            query.drawDate(),
            query.drawTime(),
            query.externalGameCodes().stream().sorted().toList(),
            SHAPE
                + "|"
                + gameCode
                + "|"
                + url
                + "|"
                + form
                + "|providerSlotCode="
                + StringUtils.defaultString(query.providerSlotCode()));

    var body =
        cache.getOrFetch(PROVIDER.name(), query.drawDate(), queryHash, () -> fetchBody(url, form));
    if (StringUtils.isBlank(body)) {
      return;
    }

    if (raw.length() > 0) {
      raw.append('\n');
    }
    raw.append("<!-- ").append(gameCode).append(" ").append(url).append(" -->\n").append(body);

    results.addAll(mapper.map(body, gameCode, Hashing.sha256Hex(body), url, query).results());
  }

  private String fetchBody(String url, String form) {
    try {
      return moLotteryRestClient
          .post()
          .uri(url)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(form)
          .retrieve()
          .body(String.class);
    } catch (Exception e) {
      log.warn("mo-client fetch failed url={} err={}", url, e.getMessage(), e);
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
