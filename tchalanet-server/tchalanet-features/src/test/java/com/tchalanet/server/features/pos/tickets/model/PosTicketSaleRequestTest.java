package com.tchalanet.server.features.pos.tickets.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.json.converter.TypedIdsJacksonModule;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

class PosTicketSaleRequestTest {

  private final JsonMapper mapper =
      JsonMapper.builder()
          .addModule(TypedIdsJacksonModule.create())
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();

  @Test
  void acceptsLegacyCashierPayloadAndBuildsCoreLines() throws Exception {
    var json =
        """
        {
          "terminalId": "legacy-terminal",
          "drawId": "80000000-0000-0000-0000-000000000001",
          "drawChannelId": "90000000-0000-0000-0000-000000000001",
          "currency": "HTG",
          "lines": [
            {
              "gameCode": "HT_BOLET",
              "betType": "MATCH_1_2D",
              "selection": "11",
              "betOption": null,
              "stake": "5.00"
            }
          ]
        }
        """;

    var request = mapper.readValue(json, PosTicketSaleRequest.class);
    var lines = request.toLines();

    assertThat(request.currencyCode().value()).isEqualTo("HTG");
    assertThat(lines).hasSize(1);
    assertThat(lines.getFirst().lineNumber()).isEqualTo(1);
    assertThat(lines.getFirst().stakeAmount()).isEqualByComparingTo("5.00");
  }
}
