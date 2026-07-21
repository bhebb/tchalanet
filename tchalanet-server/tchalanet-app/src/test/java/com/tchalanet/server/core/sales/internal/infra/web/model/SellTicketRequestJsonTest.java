package com.tchalanet.server.core.sales.internal.infra.web.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.json.converter.TypedIdsJacksonModule;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SellTicketRequestJsonTest {

  // Mirror the production stack: Jackson 3 with the typed-id module.
  private final JsonMapper objectMapper =
      JsonMapper.builder().addModule(TypedIdsJacksonModule.create()).build();

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
                "betType": "MATCH_1_2D",
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
