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
import org.hibernate.envers.Audited;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "outlet")
@Getter
@Setter
@Audited
public class OutletJpaEntity extends BaseTenantEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "slug", nullable = false)
  private String slug;

  @Column(name = "kind", nullable = false, length = 40)
  @Enumerated(EnumType.STRING)
  private OutletKind kind = OutletKind.OWNED_SHOP;

  @Column(name = "partner_ref", length = 120)
  private String partnerRef;

  @Column(name = "zone_id")
  private UUID zoneId;

  @Column(name = "metadata_json", columnDefinition = "jsonb")
  private String metadataJson;

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

  // ── Offline sales block ───────────────────────────────────────────────

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "blocked", column = @Column(name = "offline_sales_blocked", nullable = false)),
      @AttributeOverride(name = "reason",  column = @Column(name = "offline_sales_block_reason")),
      @AttributeOverride(name = "at",      column = @Column(name = "offline_sales_blocked_at")),
      @AttributeOverride(name = "by",      column = @Column(name = "offline_sales_blocked_by"))
  })
  private BlockStateJpaEmbed offlineSalesBlock = new BlockStateJpaEmbed();

  // ── Config ────────────────────────────────────────────────────────────

  @Column(name = "timezone", nullable = false)
  private String timezone = "America/Port-au-Prince";

  @Column(name = "receipt_printing_enabled", nullable = false)
  private boolean receiptPrintingEnabled = true;

  @Column(name = "receipt_header_message")
  private String receiptHeaderMessage;

  @Column(name = "receipt_footer_message")
  private String receiptFooterMessage;

  @Column(name = "require_opening_float", nullable = false)
  private boolean requireOpeningFloat = true;

  @Column(name = "auto_session_open_enabled", nullable = false)
  private boolean autoSessionOpenEnabled = false;

  @Column(name = "auto_session_close_enabled", nullable = false)
  private boolean autoSessionCloseEnabled = false;

  @Column(name = "session_open_time")
  private LocalTime sessionOpenTime;

  @Column(name = "session_close_time")
  private LocalTime sessionCloseTime;

  @Column(name = "default_opening_float_cents")
  private Long defaultOpeningFloatCents;

  @Column(name = "address_id")
  private UUID addressId;
}
