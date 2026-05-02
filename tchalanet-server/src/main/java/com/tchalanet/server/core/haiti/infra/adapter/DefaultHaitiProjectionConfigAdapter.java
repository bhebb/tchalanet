package com.tchalanet.server.core.haiti.infra.adapter;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.core.haiti.application.port.out.HaitiProjectionConfigPort;
import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiLot;
import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiProjectionConfig;
import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiProjectionToken;
import java.util.EnumMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DefaultHaitiProjectionConfigAdapter implements HaitiProjectionConfigPort {

  @Override
  public HaitiProjectionConfig getDefault() {
    var tokens = new EnumMap<HaitiLot, HaitiProjectionToken>(HaitiLot.class);
    // Exemple : LOT1..LOT4 mapping (à ajuster selon tes vraies règles)
    tokens.put(HaitiLot.LOT1, HaitiProjectionToken.PICK3_FULL_3);
    tokens.put(HaitiLot.LOT2, HaitiProjectionToken.PICK3_FIRST2);
    tokens.put(HaitiLot.LOT3, HaitiProjectionToken.PICK3_LAST2);
    tokens.put(HaitiLot.LOT4, HaitiProjectionToken.PICK3_FIRST2);
    return new HaitiProjectionConfig(tokens);
  }

  @Override
  public HaitiProjectionConfig resolve(JsonNode projectionCfg) {
    if (projectionCfg == null || projectionCfg.isNull() || projectionCfg.isMissingNode()) {
      return getDefault();
    }

    var rules = projectionCfg.path("rules");
    if (rules.isMissingNode() || rules.isNull() || !rules.isObject()) {
      return getDefault();
    }

    try {
      var tokens = new EnumMap<HaitiLot, HaitiProjectionToken>(HaitiLot.class);
      for (HaitiLot lot : HaitiLot.values()) {
        var node = rules.path(lot.name().toLowerCase());
        if (node.isMissingNode() || node.isNull()) {
          return getDefault();
        }
        tokens.put(lot, HaitiProjectionToken.parse(node.asString()));
      }
      return new HaitiProjectionConfig(tokens);
    } catch (RuntimeException e) {
      log.warn("haiti.projection_cfg invalid; falling back to default: {}", e.toString());
      return getDefault();
    }
  }
}
