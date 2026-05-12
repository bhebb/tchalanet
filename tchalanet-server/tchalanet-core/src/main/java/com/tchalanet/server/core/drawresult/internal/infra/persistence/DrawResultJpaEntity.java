package com.tchalanet.server.core.drawresult.internal.infra.persistence;

import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Audited
@Getter
@Setter
@Entity
@Table(
    name = "draw_result",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_draw_result_slot_occurred",
            columnNames = {"result_slot_id", "occurred_at"})
    },
    indexes = {
        @Index(name = "ix_draw_result_status", columnList = "status"),
        @Index(name = "ix_draw_result_slot_occurred", columnList = "result_slot_id, occurred_at"),
        @Index(name = "ix_draw_result_source_hash", columnList = "source_hash")
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
