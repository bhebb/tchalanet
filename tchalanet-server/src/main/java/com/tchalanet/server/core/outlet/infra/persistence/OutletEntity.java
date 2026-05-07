package com.tchalanet.server.core.outlet.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "outlet")
@Getter
@Setter
@Audited
public class OutletEntity extends BaseTenantEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "slug", nullable = false)
  private String slug;

  @Column(name = "day_closed", nullable = false)
  private boolean dayClosed = false;

  @Column(name = "sales_blocked", nullable = false)
  private boolean salesBlocked = false;

  @Column(name = "sales_block_reason")
  private String salesBlockReason;

  @Column(name = "sales_blocked_at")
  private Instant salesBlockedAt;

  @Column(name = "timezone", nullable = false)
  private String timezone = "America/Port-au-Prince";

  @Column(name = "business_day_cutoff")
  private LocalTime businessDayCutoff;

  @Column(name = "receipt_printing_enabled", nullable = false)
  private boolean receiptPrintingEnabled = true;

  @Column(name = "receipt_header_message")
  private String receiptHeaderMessage;

  @Column(name = "receipt_footer_message")
  private String receiptFooterMessage;

  @Column(name = "require_opening_float", nullable = false)
  private boolean requireOpeningFloat = true;

  @Column(name = "auto_open_session", nullable = false)
  private boolean autoOpenSession = false;

  @Column(name = "auto_close_session", nullable = false)
  private boolean autoCloseSession = false;

  @Column(name = "auto_session_user_id")
  private UUID autoSessionUserId;

  @Column(name = "auto_session_terminal_id")
  private UUID autoSessionTerminalId;

  @Column(name = "default_opening_float_cents")
  private Long defaultOpeningFloatCents;

  @Column(name = "address_id")
  private UUID addressId;

  // Keep entity as simple JPA bean. Conversions to/from domain happen in adapters.
}
