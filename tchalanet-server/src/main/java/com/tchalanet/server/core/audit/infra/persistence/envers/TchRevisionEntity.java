// common/infra/audit/TchRevisionEntity.java
package com.tchalanet.server.core.audit.infra.persistence.envers;

import jakarta.persistence.*;
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

  // helper
  public Instant getInstant() {
    return Instant.ofEpochMilli(timestamp);
  }
}
