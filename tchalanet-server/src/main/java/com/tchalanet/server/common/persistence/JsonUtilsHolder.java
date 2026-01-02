package com.tchalanet.server.common.persistence;

import com.tchalanet.server.common.util.JsonUtils;

/** Small holder to get JsonUtils bean from static context for JPA converters. */
public final class JsonUtilsHolder {

  private static JsonUtils INSTANCE;

  private JsonUtilsHolder() {}

  public static JsonUtils get() {
    if (INSTANCE == null) {
      INSTANCE = com.tchalanet.server.common.config.SpringContextHolder.getBean(JsonUtils.class);
    }
    return INSTANCE;
  }
}
