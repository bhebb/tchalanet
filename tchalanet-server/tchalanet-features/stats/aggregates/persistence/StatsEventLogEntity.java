package com.tchalanet.server.features.stats.aggregates.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stats_event_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsEventLogEntity {

  @Id
  @Column(name = "event_id", columnDefinition = "uuid")
  private UUID eventId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;
}
