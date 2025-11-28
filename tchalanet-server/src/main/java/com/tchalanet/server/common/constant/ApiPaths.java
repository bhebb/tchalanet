package com.tchalanet.server.common.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ApiPaths {

  public static final String API_VERSION_V1 = "v1";
  public static final String API_BASE = "/api";
  public static final String API_V1 = API_BASE + "/" + API_VERSION_V1;

  public static final String API_V1_ADMIN = API_V1 + "/admin";
  public static final String API_V1_PUBLIC = API_V1 + "/public";
}
