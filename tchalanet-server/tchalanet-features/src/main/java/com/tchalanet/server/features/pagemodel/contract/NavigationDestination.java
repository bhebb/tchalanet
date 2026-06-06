package com.tchalanet.server.features.pagemodel.contract;

import java.util.List;

/** Technical target for an {@link ActionItem}. */
public record NavigationDestination(
    String kind,
    String value,
    List<String> requiredRoles) {

  public static NavigationDestination route(String value) {
    return new NavigationDestination("route", value, List.of());
  }

  public static NavigationDestination url(String value) {
    return new NavigationDestination("url", value, List.of());
  }
}
