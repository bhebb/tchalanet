package com.tchalanet.server.platform.identity.internal.persistence.entity;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "app_user",
    indexes = @Index(name = "ix_app_user_email", columnList = "email"))
@Getter
@Setter
public class AppUserJpaEntity extends BaseEntity {

  @Column(name = "username")
  private String username;

  @Column(name = "email")
  private String email;

  @Column(name = "phone")
  private String phone;

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private UserStatus status;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "approved_by")
  private UUID approvedBy;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;
}
