package com.tchalanet.server.core.user.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.types.enums.UserStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Table(name = "app_user")
@Audited
@Getter
@Setter
public class AppUserJpaEntity extends BaseTenantEntity {

  @Column(name = "keycloak_id", nullable = false, updatable = false)
  private UUID keycloakId;

  @Column(name = "username", nullable = false)
  private String username;

  @Column(name = "email")
  private String email;

  @Column(name = "phone")
  private String phone;

  @Column(name = "tenant_code", nullable = false)
  private String tenantCode;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(name = "locale")
  private String locale;

  @Column(name = "time_zone")
  private String timeZone;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private UserStatus status;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "approved_by")
  private UUID approvedBy;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  /**
   * IMPORTANT: - Pas d'audit sur les collections (ça gonfle l'historique et casse vite). - On garde
   * le lien uniquement pour lecture/écriture quand besoin.
   */
  @NotAudited
  @OneToMany(
      mappedBy = "user",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<UserPreferenceJpaEntity> preferences = new ArrayList<>();
}
