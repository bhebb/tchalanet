package com.tchalanet.server.core.analytics.internal.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analytics_selection")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSelectionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "tenant_id", columnDefinition = "uuid", nullable = false)
  private UUID tenantId;

  @Column(name = "ref_date", nullable = false)
  private LocalDate refDate;

  @Column(name = "draw_channel_id", columnDefinition = "uuid")
  private UUID drawChannelId;

  @Column(name = "game_code", nullable = false, length = 64)
  private String gameCode;

  @Column(name = "bet_type", nullable = false, length = 64)
  private String betType;

  @Column(name = "bet_option")
  private Short betOption;

  @Column(name = "selection_key", length = 128)
  private String selectionKey;

  @Column(name = "tickets_count", nullable = false)
  private long ticketsCount;

  @Column(name = "stake_sum_cents", nullable = false)
  private long stakeSumCents;

  @Column(name = "winnings_calculated_cents", nullable = false)
  private long winningsCalculatedCents;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
