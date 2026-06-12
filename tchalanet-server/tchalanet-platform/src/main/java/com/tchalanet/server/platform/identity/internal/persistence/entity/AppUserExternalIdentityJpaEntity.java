package com.tchalanet.server.platform.identity.internal.persistence.entity;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "app_user_external_identity",
    indexes = @Index(name = "ix_app_user_external_identity__app_user", columnList = "app_user_id"),
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_app_user_external_identity__provider_issuer_subject",
            columnNames = {"provider", "issuer", "external_subject"}))
@Audited
@Getter
@Setter
public class AppUserExternalIdentityJpaEntity extends BaseEntity {

  @Column(name = "app_user_id", nullable = false, updatable = false, columnDefinition = "uuid")
  private UUID appUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 32, updatable = false)
  private IdentityProviderType provider;

  @Column(name = "issuer", nullable = false, length = 512)
  private String issuer;

  @Column(name = "external_subject", nullable = false, length = 255, updatable = false)
  private String externalSubject;

  @Column(name = "email_snapshot")
  private String emailSnapshot;
}

