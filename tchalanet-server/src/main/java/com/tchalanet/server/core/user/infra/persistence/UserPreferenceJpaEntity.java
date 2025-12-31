package com.tchalanet.server.core.user.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.common.types.enums.ThemeMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "user_preference",
    uniqueConstraints =
        @UniqueConstraint(
            name = "ux_user_preference_user_id",
            columnNames = {"user_id"}))
@Audited
@Getter
@Setter
public class UserPreferenceJpaEntity extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUserJpaEntity user;

  @Enumerated(EnumType.STRING)
  @Column(name = "theme_mode", length = 10)
  private ThemeMode themeMode;

  @Column(name = "density")
  private Short density;

  @Column(name = "locale", length = 8)
  private String locale;
}
