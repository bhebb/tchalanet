package com.tchalanet.server.common.util;

import com.tchalanet.server.common.context.SpringContextHolder;

/** Small holder to get JsonUtils bean from static context for JPA converters. */
public final class JsonUtilsHolder {

  private static JsonUtils INSTANCE;

  private JsonUtilsHolder() {}

  public static JsonUtils get() {
    if (INSTANCE == null) {
      INSTANCE = SpringContextHolder.getBean(JsonUtils.class);
    }
    return INSTANCE;
  }
}
