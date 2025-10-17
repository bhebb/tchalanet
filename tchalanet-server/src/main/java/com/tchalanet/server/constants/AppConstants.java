package com.tchalanet.server.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AppConstants {

  public static final String API_VERSION_V1 = "v1";
  public static final String API_BASE_PATH = "/api";
  public static final String API_BASE_PATH_VERSION = API_BASE_PATH + "/" + API_VERSION_V1;
  public static final String API_BASE_PATH_VERSION_ADMIN = API_BASE_PATH_VERSION + "/admin";
  public static final String API_BASE_PATH_VERSION_PUBLIC = API_BASE_PATH_VERSION + "/public";
  public static final String API_VERSION_HEADER = "X-Api-Version";
  public static final String APP_VERSION_HEADER = "X-App-Version";
  public static final String APP_HEADER_ERROR_VERSION = "X-Error-Version";
  public static final String TENANT_ID_CLAIMS = "tenant_id";
  public static final String ROLES = "roles";
  public static final String REQUEST_CONTEXT = "REQUEST_CONTEXT";
}
