package com.tchalanet.server.user.infra.persistence;

import com.tchalanet.server.common.infra.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

  @Column(name = "email", columnDefinition = "citext")
  private String email;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "locale", length = 8)
  private String locale;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "app_user_role",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<AppRoleJpaEntity> roles = new HashSet<>();

  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
  private List<UserPreferenceJpaEntity> preferences = new ArrayList<>();
}
