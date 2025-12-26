package com.tchalanet.server.features.publichome;

import jakarta.annotation.Nullable;

public final class PublicHomeSectionMapper {

  private PublicHomeSectionMapper() {}

  @Nullable
  public static PublicHomeSectionType fromRowId(String id) {
    return switch (id) {
      case "hero" -> PublicHomeSectionType.HERO;
      case "draws" -> PublicHomeSectionType.DRAWS;
      case "check_ticket" -> PublicHomeSectionType.CHECK_TICKET;
      case "features" -> PublicHomeSectionType.FEATURES;
      case "plans" -> PublicHomeSectionType.PLANS;
      case "news" -> PublicHomeSectionType.NEWS;
      case "testimonials" -> PublicHomeSectionType.TESTIMONIALS;
      case "tchala" -> PublicHomeSectionType.TCHALA;
      default -> null;
    };
  }
}
