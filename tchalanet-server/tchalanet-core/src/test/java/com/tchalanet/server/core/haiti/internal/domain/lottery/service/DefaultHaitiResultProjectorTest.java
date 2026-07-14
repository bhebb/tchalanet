package com.tchalanet.server.core.haiti.internal.domain.lottery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.core.haiti.internal.domain.lottery.model.ExternalPick;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.HaitiLot;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.HaitiProjectionConfig;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.HaitiProjectionToken;
import java.util.EnumMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DefaultHaitiResultProjector")
class DefaultHaitiResultProjectorTest {

  private final DefaultHaitiResultProjector projector = new DefaultHaitiResultProjector();

  @Test
  @DisplayName("projects pick3=456, pick4=7890 with standard config")
  void projectsStandardConfig() {
    var config = standardConfig();
    var pick = ExternalPick.of("456", "7890");

    var result = projector.project(config, pick);

    assertThat(result.lot(HaitiLot.LOT1)).isEqualTo("45");
    assertThat(result.lot(HaitiLot.LOT2)).isEqualTo("56");
    assertThat(result.lot(HaitiLot.LOT3)).isEqualTo("78");
    assertThat(result.lot(HaitiLot.LOT4)).isEqualTo("90");
  }

  @Test
  @DisplayName("projects full pick3 and pick4 when configured")
  void projectsFullPicks() {
    var tokens = new EnumMap<HaitiLot, HaitiProjectionToken>(HaitiLot.class);
    tokens.put(HaitiLot.LOT1, HaitiProjectionToken.PICK3_FULL_3);
    tokens.put(HaitiLot.LOT2, HaitiProjectionToken.PICK4_FULL_4);
    tokens.put(HaitiLot.LOT3, HaitiProjectionToken.PICK3_FIRST2);
    tokens.put(HaitiLot.LOT4, HaitiProjectionToken.PICK4_LAST2);
    var config = new HaitiProjectionConfig(tokens);
    var pick = ExternalPick.of("123", "4567");

    var result = projector.project(config, pick);

    assertThat(result.lot(HaitiLot.LOT1)).isEqualTo("123");
    assertThat(result.lot(HaitiLot.LOT2)).isEqualTo("4567");
    assertThat(result.lot(HaitiLot.LOT3)).isEqualTo("12");
    assertThat(result.lot(HaitiLot.LOT4)).isEqualTo("67");
  }

  @Test
  @DisplayName("partial pick with null pick4 produces empty strings for pick4 tokens")
  void partialPickNullPick4() {
    var pick = ExternalPick.partial("456", null);

    assertThatThrownBy(() -> projector.project(standardConfig(), pick))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static HaitiProjectionConfig standardConfig() {
    var tokens = new EnumMap<HaitiLot, HaitiProjectionToken>(HaitiLot.class);
    tokens.put(HaitiLot.LOT1, HaitiProjectionToken.PICK3_FIRST2);
    tokens.put(HaitiLot.LOT2, HaitiProjectionToken.PICK3_LAST2);
    tokens.put(HaitiLot.LOT3, HaitiProjectionToken.PICK4_FIRST2);
    tokens.put(HaitiLot.LOT4, HaitiProjectionToken.PICK4_LAST2);
    return new HaitiProjectionConfig(tokens);
  }
}
