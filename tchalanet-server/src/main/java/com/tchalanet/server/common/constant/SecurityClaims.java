package com.tchalanet.server.common.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SecurityClaims {
  public static final String TENANT_CODE = "tenant_code";
  public static final String ROLES = "roles";

  // user identity
  public static final String SUBJECT = "sub"; // Keycloak user id (UUID string)
  public static final String PREFERRED_USERNAME = "preferred_username";
  public static final String EMAIL = "email";
}
