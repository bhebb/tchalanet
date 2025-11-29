package com.tchalanet.server.core.user.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
  private List<UserPreferenceJpaEntity> preferences = new ArrayList<>();
}
