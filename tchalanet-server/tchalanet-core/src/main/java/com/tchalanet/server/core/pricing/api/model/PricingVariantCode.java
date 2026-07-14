package com.tchalanet.server.core.pricing.api.model;

/**
 * Technical pricing variant resolved from a commercial bet option and the played selection.
 *
 * <p>This is the stable inter-domain code used to key odds. Seller/POS surfaces continue to show
 * commercial {@code BetOption} labels; this code is for pricing, settlement, audit, and support.
 */
public enum PricingVariantCode {

  // Bòlèt / 2D lots
  MATCH_1_2D("1er lot"),
  MATCH_2_2D("2e lot"),
  MATCH_3_2D("3e lot"),

  // Maryaj
  MARRIAGE_EXACT_ORDER("Maryaj · ordre exact"),
  MARRIAGE_REVERSE_ALLOWED("Maryaj · revers/double"),

  // Loto 3
  LOTTO3_STRAIGHT("Exact"),
  LOTTO3_BOX_3_WAY("Permuté · 3-way"),
  LOTTO3_BOX_6_WAY("Permuté · 6-way"),

  // Loto 4
  LOTTO4_STRAIGHT("Exact"),
  LOTTO4_BOX_4_WAY("Permuté · 4-way"),
  LOTTO4_BOX_6_WAY("Permuté · 6-way"),
  LOTTO4_BOX_12_WAY("Permuté · 12-way"),
  LOTTO4_BOX_24_WAY("Permuté · 24-way"),
  LOTTO4_FRONT_PAIR("Deux premiers"),
  LOTTO4_BACK_PAIR("Deux derniers"),

  // Loto 5
  LOTTO5_LOT1_LOT2("1er + 2e lot"),
  LOTTO5_LOT1_LOT3("1er + 3e lot"),
  LOTTO5_MIXED_1_2_3("Mixte 1er/2e/3e lot");

  private final String adminLabel;

  PricingVariantCode(String adminLabel) {
    this.adminLabel = adminLabel;
  }

  /** Admin/support label. Not shown on the seller terminal or customer receipt. */
  public String adminLabel() {
    return adminLabel;
  }
}
