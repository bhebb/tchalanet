package com.tchalanet.server.core.drawresult.infra.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiLot;
import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiProjectionToken;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

class DefaultHaitiProjectionConfigAdapterTest {

  private final JsonMapper json = new JsonMapper();
  private final DefaultHaitiProjectionConfigAdapter adapter = new DefaultHaitiProjectionConfigAdapter();

  @Test
  void resolvesSlotProjectionConfigWhenValid() throws Exception {
    var cfg =
        adapter.resolve(
            json.readTree(
                """
                {
                  "version": 1,
                  "rules": {
                    "lot1": "PICK3_FULL_3",
                    "lot2": "PICK4_FIRST2",
                    "lot3": "PICK4_LAST2",
                    "lot4": "PICK3_LAST2"
                  }
                }
                """));

    assertThat(cfg.tokens().get(HaitiLot.LOT4)).isEqualTo(HaitiProjectionToken.PICK3_LAST2);
  }

  @Test
  void fallsBackToDefaultWhenSlotProjectionConfigIsIncomplete() throws Exception {
    var cfg = adapter.resolve(json.readTree("{\"rules\":{\"lot1\":\"PICK4_FULL_4\"}}"));

    assertThat(cfg.tokens().get(HaitiLot.LOT4)).isEqualTo(HaitiProjectionToken.PICK3_FIRST2);
  }
}
