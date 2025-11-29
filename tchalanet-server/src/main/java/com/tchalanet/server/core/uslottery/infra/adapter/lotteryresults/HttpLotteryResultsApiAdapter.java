package com.tchalanet.server.core.uslottery.infra.adapter.lotteryresults;

import com.tchalanet.server.core.uslottery.domain.dto.LatestDrawDto;
import com.tchalanet.server.core.uslottery.domain.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.domain.ports.out.LatestDrawProviderClient;
import com.tchalanet.server.core.uslottery.infra.adapter.lotteryresults.dto.LotteryResultsApiResponse;
import com.tchalanet.server.core.uslottery.infra.config.LotteryResultsApiProperties;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component; // Added for @Component
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class HttpLotteryResultsApiAdapter
    implements LatestDrawProviderClient { // Implements LatestDrawProviderClient

  private static final Logger log = LoggerFactory.getLogger(HttpLotteryResultsApiAdapter.class);

  private final WebClient webClient;
  private final LotteryResultsApiProperties props;

  public HttpLotteryResultsApiAdapter(LotteryResultsApiProperties props) {
    WebClient.Builder builder = WebClient.builder();
    this.props = props;
    this.webClient =
        builder
            .baseUrl(props.baseUrl())
            .defaultHeaders(headers -> headers.set("X-Api-Key", props.apiKey()))
            .build();
  }

  @Override
  public UsLotteryProvider provider() {
    // This adapter is for a generic LotteryResultsAPI, so we need to map it to a specific provider
    // For now, let's assume it's a generic "EXTERNAL_API" or similar, or configure it.
    // For simplicity, let's return Florida for now, but this needs proper mapping.
    return UsLotteryProvider.FLORIDA;
  }

  public List<LatestDrawDto> fetchLatestDraws() { // Changed return type to List<LatestDraw>
    if (!props.enabled()) {
      log.debug("LotteryResultsAPI disabled, skipping external fetch.");
      return List.of();
    }

    try {
      // The external API might return multiple draws, or we might need to query for specific ones.
      // For simplicity, let's assume it returns a list or can be adapted.
      // This part needs to be adapted based on the actual external API's behavior.
      // Assuming the API returns a list of results for various lotteries.
      List<LotteryResultsApiResponse> apiResponses =
          webClient
              .get()
              .uri("/draws") // Adjust URI based on actual API
              .retrieve()
              .bodyToFlux(LotteryResultsApiResponse.class) // Use bodyToFlux for list
              .collectList()
              .block();

      if (apiResponses == null || apiResponses.isEmpty()) {
        log.info("No results found from LotteryResultsAPI.");
        return List.of();
      }

      return apiResponses.stream().map(this::toLatestDraw).collect(Collectors.toList());

    } catch (WebClientResponseException.TooManyRequests e) {
      log.warn("Rate limit hit on LotteryResultsAPI (429)", e);
      return List.of();
    } catch (WebClientResponseException e) {
      log.error(
          "HTTP error from LotteryResultsAPI: status={} body={}",
          e.getStatusCode(),
          e.getResponseBodyAsString());
      return List.of();
    } catch (Exception e) {
      log.error("Unexpected error calling LotteryResultsAPI", e);
      return List.of();
    }
  }

  private LatestDrawDto toLatestDraw(LotteryResultsApiResponse resp) {
    // This mapping needs to be accurate based on your domain model and API response
    return null;
  }
}
