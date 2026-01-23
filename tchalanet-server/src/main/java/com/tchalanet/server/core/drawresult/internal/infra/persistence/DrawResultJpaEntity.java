package com.tchalanet.server.core.drawresult.internal.infra.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.domain.model.DrawSource;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Audited
@Getter
@Setter
@Entity
@Table(
    name = "draw_result",
    indexes = {
      @Index(name = "ix_draw_result_status", columnList = "status")
    })
public class DrawResultJpaEntity extends BaseEntity {

   @Column(name = "result_slot_id", nullable = false)
   private UUID resultSlotId; // FK vers result_slot.id

   @Column(name = "occurred_at", nullable = false)
   private Instant occurredAt;

   @Column(name = "source_result", nullable = false, columnDefinition = "jsonb")
   @JdbcTypeCode(SqlTypes.JSON)
   private JsonNode sourceResult;

   @Column(name = "haiti_result", nullable = false, columnDefinition = "jsonb")
   @JdbcTypeCode(SqlTypes.JSON)
   private JsonNode haitiResult;

   @Column(name = "raw_payload", columnDefinition = "jsonb")
   @JdbcTypeCode(SqlTypes.JSON)
   private JsonNode rawPayload;

   @Column(name = "flags", nullable = false, columnDefinition = "jsonb")
   @JdbcTypeCode(SqlTypes.JSON)
   private JsonNode flags;

   @Column(name = "status", nullable = false, length = 16)
   @Enumerated(EnumType.STRING)
   private DrawResultStatus status;

   @Column(name = "quality", length = 16)
   @Enumerated(EnumType.STRING)
   private ResultQuality quality;

   @Column(name = "source", length = 32)
   @Enumerated(EnumType.STRING)
   private DrawSource source; // API/MANUAL/IMPORT

   @Column(name = "source_hash", length = 64)
   private String sourceHash;

   @Column(name = "fetched_at", nullable = false)
   private Instant fetchedAt;

   @Column(name = "override_reason")
   private String overrideReason;
 }
