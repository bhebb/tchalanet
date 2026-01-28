package com.tchalanet.server.core.user.domain.model;

import com.tchalanet.server.common.types.enums.UserStatus;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class AppUser {

  private final UserId id;
  private final KeycloakUserSub keycloakSub;
  private final String username;

  private final String email;
  private final String phone;

  private final String firstName;
  private final String lastName;
  private final String displayName;
  private final String avatarUrl;

  private final UserStatus status;
  private final Instant approvedAt;
  private final UUID approvedBy;

  private final Instant lastLoginAt;

  private final long version;

  private AppUser(
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
      UUID approvedBy,
      Instant lastLoginAt,
      long version) {
    this.id = id;
    this.keycloakSub = keycloakSub;
    this.username = username;
    this.email = email;
    this.phone = phone;
    this.firstName = firstName;
    this.lastName = lastName;
    this.displayName = displayName;
    this.avatarUrl = avatarUrl;
    this.status = status;
    this.approvedAt = approvedAt;
    this.approvedBy = approvedBy;
    this.lastLoginAt = lastLoginAt;
    this.version = version;
  }

  // Factory "new" (V1 : PENDING_APPROVAL)
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
    if (keycloakSub == null) throw new IllegalArgumentException("keycloakSub is required");
    if (username == null || username.isBlank())
      throw new IllegalArgumentException("username is required");
    if (now == null) throw new IllegalArgumentException("now is required");

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
        now,
        0L);
  }

  // Restore depuis persistence
  public static AppUser restore(
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
      UUID approvedBy,
      Instant lastLoginAt,
      long version) {
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
      String avatarUrl) {
    return new AppUser(
        id,
        keycloakSub,
        (username != null && !username.isBlank()) ? username : this.username,
        (email != null && !email.isBlank()) ? email : this.email,
        (phone != null && !phone.isBlank()) ? phone : this.phone,
        (firstName != null) ? firstName : this.firstName,
        (lastName != null) ? lastName : this.lastName,
        (displayName != null) ? displayName : this.displayName,
        (avatarUrl != null) ? avatarUrl : this.avatarUrl,
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
        keycloakSub,
        username,
        email,
        phone,
        firstName,
        lastName,
        displayName,
        avatarUrl,
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
        keycloakSub,
        username,
        email,
        phone,
        firstName,
        lastName,
        displayName,
        avatarUrl,
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
        keycloakSub,
        username,
        email,
        phone,
        firstName,
        lastName,
        displayName,
        avatarUrl,
        UserStatus.ACTIVE,
        approvedAt,
        approvedBy,
        lastLoginAt,
        this.version);
  }

  public AppUser touchVersion() {
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
        lastLoginAt,
        this.version + 1);
  }

  public UserId getId() {
    return id;
  }

  public KeycloakUserSub getKeycloakSub() {
    return keycloakSub;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public UserStatus getStatus() {
    return status;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public UUID getApprovedBy() {
    return approvedBy;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public long getVersion() {
    return version;
  }
}
