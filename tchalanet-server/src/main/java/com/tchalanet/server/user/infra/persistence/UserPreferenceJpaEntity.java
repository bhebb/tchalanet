package com.tchalanet.server.user.infra.persistence;

import com.tchalanet.server.user.domain.model.ThemeMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_preference")
@Getter
@Setter
@NoArgsConstructor
public class UserPreferenceJpaEntity {
  @Id
  @Column(name = "user_id")
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "theme_mode")
  private ThemeMode themeMode; // nullable

  private Short density; // nullable
  private String locale; // nullable

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @PrePersist
  @PreUpdate
  void touch() {
    updatedAt = OffsetDateTime.now();
  }
}
