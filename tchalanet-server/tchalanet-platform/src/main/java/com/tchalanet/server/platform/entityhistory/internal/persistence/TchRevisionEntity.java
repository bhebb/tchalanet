package com.tchalanet.server.platform.entityhistory.internal.persistence;

import com.tchalanet.server.platform.entityhistory.internal.listener.TchRevisionListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@Table(name = "revinfo")
@RevisionEntity(TchRevisionListener.class)
@Getter
@Setter
public class TchRevisionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tch_revinfo_seq")
  @SequenceGenerator(name = "tch_revinfo_seq", sequenceName = "tch_revinfo_seq", allocationSize = 1)
  @RevisionNumber
  @Column(name = "rev")
  private Integer id;

  @RevisionTimestamp
  @Column(name = "rev_timestamp", nullable = false)
  private long timestamp;

  @Column(name = "tenant_id")
  private UUID tenantId;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "request_id", length = 128)
  private String requestId;

  @Column(name = "actor_type", length = 32)
  private String actorType = "SYSTEM";

  @Column(name = "api_scope", length = 32)
  private String apiScope;

  @Column(name = "tenant_overridden", nullable = false)
  private boolean tenantOverridden;

  public Instant getInstant() {
    return Instant.ofEpochMilli(timestamp);
  }
}
