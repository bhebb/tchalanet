package com.tchalanet.server.uslottery.infra.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.uslottery.domain.dto.LatestDrawDto;
import com.tchalanet.server.uslottery.domain.model.UsLotteryProvider;
import com.tchalanet.server.uslottery.domain.ports.out.LatestDrawProviderClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class FloridaLotteryAdapter
    implements LatestDrawProviderClient { // Implements LatestDrawProviderClient

  private static final Logger log = LoggerFactory.getLogger(FloridaLotteryAdapter.class);
  private final WebClient client;
  private final ObjectMapper mapper = new ObjectMapper();

  public FloridaLotteryAdapter(WebClient floridaLotteryWebClient) {
    this.client = floridaLotteryWebClient;
  }

  @Override
  public UsLotteryProvider provider() {
    return UsLotteryProvider.FLORIDA;
  }

  @Override
  public List<LatestDrawDto> fetchLatestDraws() {
    try {
      var results =
          client
              .get()
              .uri("/drawgamesapp/getLatestDrawGames")
              .retrieve()
              .bodyToMono(JsonNode.class)
              .block();
      if (results == null) {
        return List.of();
      }
      return convertToLatestDrawDtos(results);
    } catch (WebClientResponseException e) {
      log.warn("FL API error: status={} message={}", e.getStatusCode().value(), e.getMessage());
    } catch (Exception e) {
      log.warn("FL adapter failed to fetch results: {}", e.toString());
    }
    return List.of();
  }

  private List<LatestDrawDto> convertToLatestDrawDtos(JsonNode results) {
    List<LatestDrawDto> latestDraws = new ArrayList<>();
    // This conversion logic needs to be implemented based on the actual JSON structure
    // of the Florida Lottery API response.
    // For demonstration, let's assume a simple structure.
    if (results.isArray()) {
      for (JsonNode node : results) {
        // Example parsing, adjust according to actual API response
        String gameName = node.path("gameName").asText();
        String drawDateStr = node.path("drawDate").asText();
        String numbersStr = node.path("winningNumbers").asText(); // Assuming comma-separated

        // Build a minimal DTO: externalChannelCode, scheduledAt (Instant), resultPayloadJson
        Instant scheduledAt = Instant.now();
        String resultPayloadJson = node.toString();
        latestDraws.add(new LatestDrawDto(gameName, scheduledAt, resultPayloadJson));
      }
    }
    return latestDraws;
  }
}
