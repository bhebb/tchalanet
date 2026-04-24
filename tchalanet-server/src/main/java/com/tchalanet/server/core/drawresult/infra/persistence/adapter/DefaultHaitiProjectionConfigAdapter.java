package com.tchalanet.server.core.drawresult.infra.persistence.adapter;

import com.tchalanet.server.core.drawresult.application.port.out.HaitiProjectionConfigPort;
import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiLot;
import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiProjectionConfig;
import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiProjectionToken;
import java.util.EnumMap;
import org.springframework.stereotype.Component;

@Component
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
}
