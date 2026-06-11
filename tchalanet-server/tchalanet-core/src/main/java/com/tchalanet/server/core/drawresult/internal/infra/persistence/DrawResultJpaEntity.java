package com.tchalanet.server.core.drawresult.internal.infra.persistence;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Audited
@Getter
@Setter
@Entity
@Table(
    name = "draw_result",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_draw_result_slot_occurred",
            columnNames = {"result_slot_id", "occurred_at"}),
        @UniqueConstraint(
            name = "uq_draw_result_slot_result_date",
            columnNames = {"result_slot_id", "result_date"})
    },
    indexes = {
        @Index(name = "ix_draw_result_status", columnList = "status"),
        @Index(name = "ix_draw_result_slot_occurred", columnList = "result_slot_id, occurred_at"),
        @Index(name = "ix_draw_result_slot_result_date", columnList = "result_slot_id, result_date"),
        @Index(name = "ix_draw_result_source_hash", columnList = "source_hash")
    })
public class DrawResultJpaEntity extends BaseEntity {

    @Column(name = "result_slot_id", nullable = false)
    private UUID resultSlotId; // FK vers result_slot.id

    // Instant exact du tirage en UTC.
// Calculé à partir de resultDate + result_slot.drawTime + result_slot.timezone.
// Exemple : NY_EVE du 2024-06-01 à 22:30 America/New_York
// devient 2024-06-02T02:30:00Z.
// Sert au tri temporel global et aux comparaisons entre slots/timezones.
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    // Date métier du résultat.
// Représente le jour de tirage dans le calendrier du slot/provider.
// Exemple : NY_EVE du 2024-06-01, même si occurredAt tombe le 2024-06-02 en UTC.
    @Column(name = "result_date", nullable = false)
    private LocalDate resultDate;

    // Moment où Tchalanet a récupéré/importé/écrit ce résultat.
// Technique : audit, debug, "dernière mise à jour", retry, diagnostics.
// Ne doit pas être utilisé comme date principale d’affichage public.
    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;


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

    @Column(name = "override_reason")
    private String overrideReason;
}
