package com.tchalanet.server.platform.identity.internal.persistence.entity;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.catalog.theme.api.ThemeMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "user_preference",
    uniqueConstraints = @UniqueConstraint(name = "ux_user_preference_user_id", columnNames = "user_id"))
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
  private Locale locale;

  @Column(name = "time_zone", length = 64)
  private ZoneId timeZone;

  @Column(name = "currency", length = 3)
  private Currency currency;
}
