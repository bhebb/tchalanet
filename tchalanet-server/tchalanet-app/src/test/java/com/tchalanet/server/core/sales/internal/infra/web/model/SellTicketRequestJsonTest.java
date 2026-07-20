package com.tchalanet.server.core.sales.internal.infra.web.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SellTicketRequestJsonTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void deserializesTheDocumentedPreparePayload() throws Exception {
    var request =
        objectMapper.readValue(
            """
            {
              "drawId": "c4f01cd6-836d-47d8-95e5-9dfc4a463d4d",
              "drawChannelId": "234c1c27-1ee1-4ab3-9295-0c4944cde4ba",
              "currency": { "value": "HTG" },
              "lines": [{
                "lineNumber": 1,
                "gameCode": "HT_BOLET",
                "betType": "SHORT_SINGLE_GAME",
                "selection": "12",
                "stakeAmount": 15.0
              }]
            }
            """,
            SellTicketRequest.class);

    assertThat(request.drawId().toString()).isEqualTo("c4f01cd6-836d-47d8-95e5-9dfc4a463d4d");
    assertThat(request.drawChannelId().toString())
        .isEqualTo("234c1c27-1ee1-4ab3-9295-0c4944cde4ba");
    assertThat(request.currency().code()).isEqualTo("HTG");
    assertThat(request.lines()).hasSize(1);
  }
}
