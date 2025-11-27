package com.tchalanet.server.uslottery.infra.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.uslottery.domain.dto.LatestDrawDto;
import com.tchalanet.server.uslottery.domain.model.UsLotteryProvider;
import com.tchalanet.server.uslottery.domain.ports.out.LatestDrawProviderClient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class NyLotteryAdapter
    implements LatestDrawProviderClient { // Implements LatestDrawProviderClient

  private static final Logger log = LoggerFactory.getLogger(NyLotteryAdapter.class);
  private final WebClient client;
  private final ObjectMapper mapper = new ObjectMapper();

  public NyLotteryAdapter(WebClient nyLotteryWebClient) {
    this.client = nyLotteryWebClient;
  }

  @Override
  public UsLotteryProvider provider() {
    return UsLotteryProvider.NY;
  }

  @Override
  public List<LatestDrawDto> fetchLatestDraws() {
    // This method needs to be adapted to fetch for all supported channel codes or a range of dates.
    // For now, let's assume it fetches for a recent date and all NY channels.
    LocalDate date = LocalDate.now(); // Fetch for today
    List<LatestDrawDto> results = new ArrayList<>();

    // Example: Fetch for NY_MID and NY_EVE
    for (String channelCode : List.of("NY_MID", "NY_EVE")) { // Assuming these are the channel codes
      try {
        String body =
            client
                .get()
                .uri(
                    uriBuilder ->
                        uriBuilder
                            .queryParam("$limit", 50)
                            .queryParam("$order", "draw_date DESC")
                            .queryParam(
                                "$where", "draw_date='" + date + "' AND game_type='NUMBERS'")
                            .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (body == null) {
          continue;
        }

        JsonNode arr = mapper.readTree(body);
        if (!arr.isArray() || arr.isEmpty()) {
          continue;
        }

        String want = channelCode.toUpperCase();
        for (JsonNode node : arr) {
          String drawTime = node.path("draw_time").asText(null);
          if (drawTime == null) {
            drawTime = node.path("drawing_time").asText(null);
          }
          boolean timeMatch = false;
          if (drawTime != null) {
            String dt = drawTime.toUpperCase();
            if (want.contains("MID") && dt.contains("MID")) timeMatch = true;
            if (want.contains("EVE") && (dt.contains("EVE") || dt.contains("EVEN")))
              timeMatch = true;
          }
          if (!timeMatch && arr.size() > 1) continue;

          List<String> numbers = extractNumbers(node);
          if (numbers.isEmpty()) continue;

          String externalChannelCode = channelCode;
          Instant scheduledAt = OffsetDateTime.now().toInstant();
          String resultPayloadJson = mapper.convertValue(node, Map.class).toString();
          results.add(new LatestDrawDto(externalChannelCode, scheduledAt, resultPayloadJson));
          break; // Found result for this channel, move to next
        }
      } catch (WebClientResponseException e) {
        log.warn(
            "NY API error: status={} channel={} date={} message={}",
            e.getStatusCode().value(),
            channelCode,
            date,
            e.getMessage());
      } catch (Exception e) {
        log.warn("NY adapter failed to fetch result for {}: {}", channelCode, e.toString());
      }
    }
    return results;
  }

  private List<String> extractNumbers(JsonNode node) {
    List<String> res = new ArrayList<>();
    String[] candidates = {
      "winning_numbers", "winning_numbers_text", "numbers", "winning_number", "winning_numbers_1"
    };
    for (String f : candidates) {
      JsonNode v = node.path(f);
      if (v.isMissingNode()) continue;
      if (v.isTextual()) {
        String s = v.asText();
        String[] parts = s.split("\\D+");
        for (String p : parts) {
          if (p.isBlank()) continue;
          res.add(p);
        }
        if (!res.isEmpty()) return res;
      }
      if (v.isArray()) {
        for (JsonNode it : v) {
          res.add(it.asText());
        }
        if (!res.isEmpty()) return res;
      }
    }
    Iterator<String> fieldNames = node.fieldNames();
    while (fieldNames.hasNext()) {
      String name = fieldNames.next();
      if (name.toLowerCase().contains("number")) {
        JsonNode v = node.path(name);
        if (v.isTextual()) {
          String[] parts = v.asText().split("\\D+");
          for (String p : parts) if (!p.isBlank()) res.add(p);
        } else if (v.isArray()) {
          for (JsonNode it : v) res.add(it.asText());
        }
      }
    }
    return res;
  }
}
