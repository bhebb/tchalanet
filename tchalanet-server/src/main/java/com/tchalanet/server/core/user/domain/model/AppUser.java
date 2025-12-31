package com.tchalanet.server.core.user.domain.model;

import com.tchalanet.server.common.types.enums.UserStatus;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class AppUser {

  private final UserId id;
  private final UUID keycloakId;
  private final String username;

  private final String email;
  private final String phone;

  private final String firstName;
  private final String lastName;
  private final String displayName;
  private final String avatarUrl;

  private final String locale;
  private final String timeZone;

  private final UserStatus status;
  private final Instant approvedAt;
  private final UUID approvedBy;

  private final Instant lastLoginAt;

  private final long version;

  private AppUser(
      UserId id,
      UUID keycloakId,
      String username,
      String email,
      String phone,
      String firstName,
      String lastName,
      String displayName,
      String avatarUrl,
      String locale,
      String timeZone,
      UserStatus status,
      Instant approvedAt,
      UUID approvedBy,
      Instant lastLoginAt,
      long version) {
    this.id = id;
    this.keycloakId = keycloakId;
    this.username = username;
    this.email = email;
    this.phone = phone;
    this.firstName = firstName;
    this.lastName = lastName;
    this.displayName = displayName;
    this.avatarUrl = avatarUrl;
    this.locale = locale;
    this.timeZone = timeZone;
    this.status = status;
    this.approvedAt = approvedAt;
    this.approvedBy = approvedBy;
    this.lastLoginAt = lastLoginAt;
    this.version = version;
  }

  // Factory "new" (V1 : PENDING_APPROVAL)
  public static AppUser createNew(
      UserId id,
      UUID keycloakId,
      String username,
      String email,
      String phone,
      String firstName,
      String lastName,
      String displayName,
      String avatarUrl,
      String locale,
      String timeZone,
      Instant now) {
    if (keycloakId == null) throw new IllegalArgumentException("keycloakId is required");
    if (username == null || username.isBlank())
      throw new IllegalArgumentException("username is required");
    if (now == null) throw new IllegalArgumentException("now is required");

    return new AppUser(
        id,
        keycloakId,
        username,
        email,
        phone,
        firstName,
        lastName,
        displayName,
        avatarUrl,
        locale,
        timeZone,
        UserStatus.PENDING_APPROVAL,
        null,
        null,
        now,
        0L);
  }

  // Restore depuis persistence
  public static AppUser restore(
      UserId id,
      UUID keycloakId,
      String username,
      String email,
      String phone,
      String firstName,
      String lastName,
      String displayName,
      String avatarUrl,
      String locale,
      String timeZone,
      UserStatus status,
      Instant approvedAt,
      UUID approvedBy,
      Instant lastLoginAt,
      long version) {
    return new AppUser(
        id,
        keycloakId,
        username,
        email,
        phone,
        firstName,
        lastName,
        displayName,
        avatarUrl,
        locale,
        timeZone,
        status,
        approvedAt,
        approvedBy,
        lastLoginAt,
        version);
  }

  // ---- Métier: returns new instance (immutability) ----

  public AppUser syncProfile(
      String username,
      String email,
      String phone,
      String firstName,
      String lastName,
      String displayName,
      String avatarUrl,
      String locale,
      String timeZone) {
    return new AppUser(
        id,
        keycloakId,
        (username != null && !username.isBlank()) ? username : this.username,
        (email != null && !email.isBlank()) ? email : this.email,
        (phone != null && !phone.isBlank()) ? phone : this.phone,
        (firstName != null) ? firstName : this.firstName,
        (lastName != null) ? lastName : this.lastName,
        (displayName != null) ? displayName : this.displayName,
        (avatarUrl != null) ? avatarUrl : this.avatarUrl,
        (locale != null && !locale.isBlank()) ? locale : this.locale,
        (timeZone != null && !timeZone.isBlank()) ? timeZone : this.timeZone,
        status,
        approvedAt,
        approvedBy,
        lastLoginAt,
        this.version);
  }

  public AppUser touchLogin(Instant now) {
    if (now == null) throw new IllegalArgumentException("now is required");
    return new AppUser(
        id,
        keycloakId,
        username,
        email,
        phone,
        firstName,
        lastName,
        displayName,
        avatarUrl,
        locale,
        timeZone,
        status,
        approvedAt,
        approvedBy,
        now,
        this.version);
  }

  public AppUser approve(Instant now, UUID approvedBy) {
    if (now == null) throw new IllegalArgumentException("now is required");
    if (approvedBy == null) throw new IllegalArgumentException("approvedBy is required");
    if (status == UserStatus.SUSPENDED)
      throw new IllegalStateException("Cannot approve suspended user");
    return new AppUser(
        id,
        keycloakId,
        username,
        email,
        phone,
        firstName,
        lastName,
        displayName,
        avatarUrl,
        locale,
        timeZone,
        UserStatus.ACTIVE,
        now,
        approvedBy,
        lastLoginAt,
        this.version);
  }

  public AppUser suspend() {
    if (status == UserStatus.SUSPENDED) return this;
    return new AppUser(
        id,
        keycloakId,
        username,
        email,
        phone,
        firstName,
        lastName,
        displayName,
        avatarUrl,
        locale,
        timeZone,
        UserStatus.SUSPENDED,
        approvedAt,
        approvedBy,
        lastLoginAt,
        this.version);
  }

  public AppUser reactivate() {
    if (status != UserStatus.SUSPENDED) return this;
    return new AppUser(
        id,
        keycloakId,
        username,
        email,
        phone,
        firstName,
        lastName,
        displayName,
        avatarUrl,
        locale,
        timeZone,
        UserStatus.ACTIVE,
        approvedAt,
        approvedBy,
        lastLoginAt,
        this.version);
  }
}
