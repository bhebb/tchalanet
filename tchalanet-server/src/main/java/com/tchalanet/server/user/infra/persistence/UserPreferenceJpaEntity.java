package com.tchalanet.server.user.infra.persistence;

import com.tchalanet.server.common.infra.persistence.BaseEntity;
import com.tchalanet.server.user.domain.model.ThemeMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "user_preference")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class UserPreferenceJpaEntity extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUserJpaEntity user;

  @Enumerated(EnumType.STRING)
  @Column(name = "theme_mode")
  private ThemeMode themeMode; // nullable

  private Short density; // nullable
  private String locale; // nullable
}
