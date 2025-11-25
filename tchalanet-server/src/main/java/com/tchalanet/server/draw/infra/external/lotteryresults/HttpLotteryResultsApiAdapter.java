package com.tchalanet.server.draw.infra.external.lotteryresults;

import com.tchalanet.server.draw.application.port.out.ExternalDrawResultPort;
import com.tchalanet.server.draw.infra.external.lotteryresults.dto.LotteryResultsApiResponse;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class HttpLotteryResultsApiAdapter implements ExternalDrawResultPort {

  private static final Logger log = LoggerFactory.getLogger(HttpLotteryResultsApiAdapter.class);

  private final WebClient webClient;
  private final LotteryResultsApiProperties props;

  public HttpLotteryResultsApiAdapter(
      WebClient.Builder builder, LotteryResultsApiProperties props) {
    this.props = props;
    this.webClient =
        builder
            .baseUrl(props.baseUrl())
            .defaultHeaders(headers -> headers.set("X-Api-Key", props.apiKey()))
            .build();
  }

  @Override
  public Optional<ExternalDrawResult> fetchResult(DrawExternalQuery query) {
    if (!props.enabled()) {
      log.debug(
          "LotteryResultsAPI disabled, skipping external fetch for {} / {}",
          query.channelCode(),
          query.drawDate());
      return Optional.empty();
    }

    try {
      LotteryResultsApiResponse resp =
          webClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/draws")
                          .queryParam("lottery", mapChannelToProvider(query.channelCode()))
                          .queryParam("date", formatDate(query.drawDate()))
                          .build())
              .retrieve()
              .bodyToMono(LotteryResultsApiResponse.class)
              .block();

      if (resp == null || resp.getNumbers() == null || resp.getNumbers().isEmpty()) {
        log.info(
            "No result found from LotteryResultsAPI for {} / {}",
            query.channelCode(),
            query.drawDate());
        return Optional.empty();
      }

      ExternalDrawResult result =
          new ExternalDrawResult(
              query.channelCode(),
              resp.getDate() != null ? resp.getDate() : query.drawDate(),
              resp.getNumbers(),
              Map.of(
                  "provider",
                  "LotteryResultsAPI",
                  "lottery",
                  resp.getLottery(),
                  "raw",
                  resp.getRaw()));

      return Optional.of(result);

    } catch (WebClientResponseException.TooManyRequests e) {
      log.warn(
          "Rate limit hit on LotteryResultsAPI (429) for {} / {}",
          query.channelCode(),
          query.drawDate());
      return Optional.empty();
    } catch (WebClientResponseException e) {
      log.error(
          "HTTP error from LotteryResultsAPI: status={} body={}",
          e.getStatusCode(),
          e.getResponseBodyAsString());
      return Optional.empty();
    } catch (Exception e) {
      log.error("Unexpected error calling LotteryResultsAPI", e);
      return Optional.empty();
    }
  }

  private String mapChannelToProvider(String channelCode) {
    return channelCode;
  }

  private String formatDate(LocalDate date) {
    return date.toString();
  }
}
