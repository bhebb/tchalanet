package com.tchalanet.server.features.pos.home.model;

public record HomeHeader(String title, String subtitle, String displayName) {
  public static HomeHeader of(String title, String subtitle) {
    return new HomeHeader(title, subtitle, null);
  }
}
