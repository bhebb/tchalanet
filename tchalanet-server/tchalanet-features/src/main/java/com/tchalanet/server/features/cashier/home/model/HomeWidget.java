package com.tchalanet.server.features.cashier.home.model;

import java.util.Map;

public record HomeWidget(String key, String title, String type, Map<String, Object> data) {
  public static HomeWidget untitled(String key, String type, Map<String, Object> data) {
    return new HomeWidget(key, null, type, data);
  }
}
