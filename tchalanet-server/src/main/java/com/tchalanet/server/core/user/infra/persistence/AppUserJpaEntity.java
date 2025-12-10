package com.tchalanet.server.core.user.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "app_user")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class AppUserJpaEntity extends BaseTenantEntity {
  @Column(name = "username", nullable = false)
  private String username;

  @Column(name = "keycloak_id")
  private UUID keycloakId;

  @Column(name = "email", columnDefinition = "citext")
  private String email;

  @Column(name = "phone", length = 32)
  private String phone;

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(name = "locale", length = 8)
  private String locale;

  @Column(name = "time_zone", length = 64)
  private String timeZone;

  @Column(name = "status", length = 32)
  private String status;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
  private List<UserPreferenceJpaEntity> preferences = new ArrayList<>();
}
