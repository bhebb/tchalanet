package com.tchalanet.server.core.outlet.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletKind;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.LocalTime;
import java.util.UUID;

@Audited
@Entity
@Table(name = "outlet")
@Getter
@Setter
public class OutletJpaEntity extends BaseTenantEntity {

  @NotAudited
  @Column(name = "name", nullable = false)
  private String name;

  @NotAudited
  @Column(name = "slug", nullable = false)
  private String slug;

  @NotAudited
  @Column(name = "kind", nullable = false, length = 40)
  @Enumerated(EnumType.STRING)
  private OutletKind kind = OutletKind.OWNED_SHOP;

  @NotAudited
  @Column(name = "partner_ref", length = 120)
  private String partnerRef;

  @NotAudited
  @Column(name = "zone_id")
  private UUID zoneId;

  @NotAudited
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata_json", columnDefinition = "jsonb")
  private JsonNode metadataJson;

  @Column(name = "status", nullable = false, length = 40)
  @Enumerated(EnumType.STRING)
  private OutletStatus status = OutletStatus.DRAFT;

  @Column(name = "day_closed", nullable = false)
  private boolean dayClosed = false;

  // ── Outlet-level block ────────────────────────────────────────────────

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "blocked", column = @Column(name = "outlet_blocked", nullable = false)),
      @AttributeOverride(name = "reason",  column = @Column(name = "outlet_block_reason")),
      @AttributeOverride(name = "at",      column = @Column(name = "outlet_blocked_at")),
      @AttributeOverride(name = "by",      column = @Column(name = "outlet_blocked_by"))
  })
  private BlockStateJpaEmbed outletBlock = new BlockStateJpaEmbed();

  // ── Sales block ───────────────────────────────────────────────────────

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "blocked", column = @Column(name = "sales_blocked", nullable = false)),
      @AttributeOverride(name = "reason",  column = @Column(name = "sales_block_reason")),
      @AttributeOverride(name = "at",      column = @Column(name = "sales_blocked_at")),
      @AttributeOverride(name = "by",      column = @Column(name = "sales_blocked_by"))
  })
  private BlockStateJpaEmbed salesBlock = new BlockStateJpaEmbed();

  // ── Payout block ──────────────────────────────────────────────────────

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "blocked", column = @Column(name = "payout_blocked", nullable = false)),
      @AttributeOverride(name = "reason",  column = @Column(name = "payout_block_reason")),
      @AttributeOverride(name = "at",      column = @Column(name = "payout_blocked_at")),
      @AttributeOverride(name = "by",      column = @Column(name = "payout_blocked_by"))
  })
  private BlockStateJpaEmbed payoutBlock = new BlockStateJpaEmbed();

  // ── Offline sales block — not audited ─────────────────────────────────

  @NotAudited
  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "blocked", column = @Column(name = "offline_sales_blocked", nullable = false)),
      @AttributeOverride(name = "reason",  column = @Column(name = "offline_sales_block_reason")),
      @AttributeOverride(name = "at",      column = @Column(name = "offline_sales_blocked_at")),
      @AttributeOverride(name = "by",      column = @Column(name = "offline_sales_blocked_by"))
  })
  private BlockStateJpaEmbed offlineSalesBlock = new BlockStateJpaEmbed();

  // ── Config — not audited ───────────────────────────────────────────────

  @NotAudited
  @Column(name = "timezone", nullable = false)
  private String timezone = "America/Port-au-Prince";

  @NotAudited
  @Column(name = "receipt_printing_enabled", nullable = false)
  private boolean receiptPrintingEnabled = true;

  @NotAudited
  @Column(name = "receipt_header_message")
  private String receiptHeaderMessage;

  @NotAudited
  @Column(name = "receipt_footer_message")
  private String receiptFooterMessage;

  @NotAudited
  @Column(name = "require_opening_float", nullable = false)
  private boolean requireOpeningFloat = true;

  @NotAudited
  @Column(name = "auto_session_open_enabled", nullable = false)
  private boolean autoSessionOpenEnabled = false;

  @NotAudited
  @Column(name = "auto_session_close_enabled", nullable = false)
  private boolean autoSessionCloseEnabled = false;

  @NotAudited
  @Column(name = "session_open_time")
  private LocalTime sessionOpenTime;

  @NotAudited
  @Column(name = "session_close_time")
  private LocalTime sessionCloseTime;

  @NotAudited
  @Column(name = "default_opening_float_cents")
  private Long defaultOpeningFloatCents;

  @NotAudited
  @Column(name = "address_id")
  private UUID addressId;
}
