package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record AppUser(
    UserId id,
    KeycloakUserSub keycloakSub,
    String username,
    String email,
    String phone,
    String firstName,
    String lastName,
    String displayName,
    String avatarUrl,
    UserStatus status,
    Instant approvedAt,
    UserId approvedBy,
    Instant lastLoginAt) {

  public static AppUser createNew(
      UserId id,
      KeycloakUserSub keycloakSub,
      String username,
      String email,
      String phone,
      String firstName,
      String lastName,
      String displayName,
      String avatarUrl,
      Instant now) {
    return new AppUser(
        id,
        keycloakSub,
        username,
        email,
        phone,
        firstName,
        lastName,
        displayName,
        avatarUrl,
        UserStatus.PENDING_APPROVAL,
        null,
        null,
        now);
  }

  public AppUser syncProfile(
      String username,
      String email,
      String phone,
      String firstName,
      String lastName,
      String displayName,
      String avatarUrl) {
    return new AppUser(
        id,
        keycloakSub,
        nonBlankOr(username, this.username),
        nonBlankOr(email, this.email),
        nonBlankOr(phone, this.phone),
        firstName != null ? firstName : this.firstName,
        lastName != null ? lastName : this.lastName,
        displayName != null ? displayName : this.displayName,
        avatarUrl != null ? avatarUrl : this.avatarUrl,
        status,
        approvedAt,
        approvedBy,
        lastLoginAt);
  }

  public AppUser touchLogin(Instant now) {
    return new AppUser(
        id,
        keycloakSub,
        username,
        email,
        phone,
        firstName,
        lastName,
        displayName,
        avatarUrl,
        status,
        approvedAt,
        approvedBy,
        now);
  }

  public AppUser approve(Instant now, UserId approvedBy) {
    return withStatus(UserStatus.ACTIVE, now, approvedBy);
  }

  public AppUser suspend() {
    return withStatus(UserStatus.SUSPENDED, approvedAt, approvedBy);
  }

  public AppUser reactivate() {
    return withStatus(UserStatus.ACTIVE, approvedAt, approvedBy);
  }

  private AppUser withStatus(UserStatus status, Instant approvedAt, UserId approvedBy) {
    return new AppUser(
        id,
        keycloakSub,
        username,
        email,
        phone,
        firstName,
        lastName,
        displayName,
        avatarUrl,
        status,
        approvedAt,
        approvedBy,
        lastLoginAt);
  }

  private static String nonBlankOr(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
