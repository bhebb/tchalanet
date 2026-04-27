package com.tchalanet.server.core.haiti.domain.lottery.service;

import com.tchalanet.server.common.contracts.haiti.HaitiResult;
import com.tchalanet.server.core.haiti.domain.lottery.model.ExternalPick;
import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiLot;
import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiProjectionConfig;
import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiProjectionToken;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public final class DefaultHaitiResultProjector implements HaitiResultProjector {

  @Override
  public HaitiResult project(HaitiProjectionConfig config, ExternalPick pick) {
    Map<HaitiLot, String> out = new LinkedHashMap<>();
    var p3 = pick.pick3();
    var p4 = pick.pick4();

    for (HaitiLot lot : HaitiLot.values()) {
      HaitiProjectionToken token = config.tokens().get(lot);
      if (token == null) throw new IllegalArgumentException("Missing token for " + lot);
      String value =
          switch (token) {
            case PICK3_FULL_3 -> require(p3, "pick3");
            case PICK3_FIRST2 -> firstN(require(p3, "pick3"), 2);
            case PICK3_LAST2 -> lastN(require(p3, "pick3"), 2);
            case PICK4_FULL_4 -> require(p4, "pick4");
            case PICK4_FIRST2 -> firstN(require(p4, "pick4"), 2);
            case PICK4_LAST2 -> lastN(require(p4, "pick4"), 2);
          };
      out.put(lot, value);
    }
    log.debug(
        "Projected Haiti result for pick3: {}, pick4: {}, pick: {}, result: {}", p3, p4, pick, out);
    return new HaitiResult(out);
  }

  private static String require(String s, String field) {
    if (s == null || s.isBlank()) throw new IllegalArgumentException("Missing " + field);
    return s.trim();
  }

  private static String firstN(String s, int n) {
    if (s.length() < n) throw new IllegalArgumentException("String too short for firstN: " + s);
    return s.substring(0, n);
  }

  private static String lastN(String s, int n) {
    if (s.length() < n) throw new IllegalArgumentException("String too short for lastN: " + s);
    return s.substring(s.length() - n);
  }
}
