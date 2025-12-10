package com.tchalanet.server.core.user.application.query.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class UserProfileQuery {
  public UUID id;
  public UUID keycloakId;
  public UUID tenantId;
  public UUID outletId;
  public String username;
  public String name;
  public String firstName;
  public String lastName;
  public String phone;
  public String email;
  public String status;
  public String displayName;
  public Set<String> roles;
  public String locale;
  public String timeZone;
  public Instant lastLoginAt;
}
