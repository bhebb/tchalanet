package com.tchalanet.server.features.publicdrawresults;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DrawChannelLabelKeyResolverTest {

  @Test
  void resolve_withValidSlotKeys_returnsCorrectI18nKeys() {
    // New York
    assertThat(DrawChannelLabelKeyResolver.resolve("NY_MID"))
        .isEqualTo("draw_channel.ny.mid.label");
    assertThat(DrawChannelLabelKeyResolver.resolve("NY_EVE"))
        .isEqualTo("draw_channel.ny.eve.label");

    // Florida
    assertThat(DrawChannelLabelKeyResolver.resolve("FL_MID"))
        .isEqualTo("draw_channel.fl.mid.label");
    assertThat(DrawChannelLabelKeyResolver.resolve("FL_EVE"))
        .isEqualTo("draw_channel.fl.eve.label");

    // Georgia
    assertThat(DrawChannelLabelKeyResolver.resolve("GA_MID"))
        .isEqualTo("draw_channel.ga.mid.label");
    assertThat(DrawChannelLabelKeyResolver.resolve("GA_EVE"))
        .isEqualTo("draw_channel.ga.eve.label");
    assertThat(DrawChannelLabelKeyResolver.resolve("GA_LATE"))
        .isEqualTo("draw_channel.ga.late.label");

    // Texas (avec horaires spécifiques)
    assertThat(DrawChannelLabelKeyResolver.resolve("TX_1000"))
        .isEqualTo("draw_channel.tx.1000.label");
    assertThat(DrawChannelLabelKeyResolver.resolve("TX_1227"))
        .isEqualTo("draw_channel.tx.1227.label");
    assertThat(DrawChannelLabelKeyResolver.resolve("TX_1800"))
        .isEqualTo("draw_channel.tx.1800.label");
    assertThat(DrawChannelLabelKeyResolver.resolve("TX_2212"))
        .isEqualTo("draw_channel.tx.2212.label");
  }

  @Test
  void resolve_withLowerCaseSlotKey_normalizesCorrectly() {
    assertThat(DrawChannelLabelKeyResolver.resolve("ny_mid"))
        .isEqualTo("draw_channel.ny.mid.label");
    assertThat(DrawChannelLabelKeyResolver.resolve("Fl_Eve"))
        .isEqualTo("draw_channel.fl.eve.label");
  }

  @Test
  void resolve_withWhitespace_trimsAndResolves() {
    assertThat(DrawChannelLabelKeyResolver.resolve("  NY_MID  "))
        .isEqualTo("draw_channel.ny.mid.label");
  }

  @Test
  void resolve_withNullOrBlank_returnsNull() {
    assertThat(DrawChannelLabelKeyResolver.resolve(null)).isNull();
    assertThat(DrawChannelLabelKeyResolver.resolve("")).isNull();
    assertThat(DrawChannelLabelKeyResolver.resolve("   ")).isNull();
  }

  @Test
  void resolve_withInvalidFormat_returnsNull() {
    // Pas d'underscore
    assertThat(DrawChannelLabelKeyResolver.resolve("NYEVENING")).isNull();

    // Seulement provider sans period
    assertThat(DrawChannelLabelKeyResolver.resolve("NY")).isNull();
  }

  @Test
  void resolve_withComplexPeriod_handlesCorrectly() {
    // Period avec plusieurs underscores (devrait prendre tout après le premier)
    assertThat(DrawChannelLabelKeyResolver.resolve("CA_DAILY_DRAW"))
        .isEqualTo("draw_channel.ca.daily_draw.label");
  }
}

