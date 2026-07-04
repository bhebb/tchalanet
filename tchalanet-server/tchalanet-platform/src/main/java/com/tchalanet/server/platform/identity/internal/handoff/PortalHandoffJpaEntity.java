package com.tchalanet.server.platform.identity.internal.handoff;

import com.tchalanet.server.common.persistence.BaseEntity;
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

@Entity
@Table(
    name = "portal_auth_handoff",
    indexes = {
        @Index(name = "ix_portal_auth_handoff__expires_at", columnList = "expires_at"),
        @Index(name = "ix_portal_auth_handoff__subject_user", columnList = "subject_user_id")
    })
@Getter
@Setter
class PortalHandoffJpaEntity extends BaseEntity {

  @Column(name = "code_hash", nullable = false, length = 64)
  private String codeHash;

  @Column(name = "subject_user_id", nullable = false, columnDefinition = "uuid")
  private UUID subjectUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_portal", nullable = false, length = 32)
  private PortalTarget targetPortal;

  @Column(name = "entry_route", nullable = false, length = 512)
  private String entryRoute;

  @Column(name = "support_access_session_id", columnDefinition = "uuid")
  private UUID supportAccessSessionId;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "consumed_at")
  private Instant consumedAt;
}
