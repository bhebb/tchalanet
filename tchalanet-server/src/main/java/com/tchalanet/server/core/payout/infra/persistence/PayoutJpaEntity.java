package com.tchalanet.server.core.payout.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "payout")
@Audited
@Getter
@Setter
public class PayoutJpaEntity extends BaseTenantEntity {

  @Column(name = "ticket_id", nullable = false)
  private UUID ticketId;

  @Column(name = "outlet_id")
  private UUID outletId;

  @Column(name = "session_id")
  private UUID sessionId;

  @Column(name = "terminal_id")
  private UUID terminalId;

  @Column(name = "paid_by_user_id")
  private UUID paidByUserId;

  @Column(name = "selling_outlet_id")
  private UUID sellingOutletId;

  @Column(name = "selling_session_id")
  private UUID sellingSessionId;

  @Column(name = "amount_cents", nullable = false)
  private Long amountCents;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(name = "rejected_at")
  private Instant rejectedAt;

  @Column(name = "rejected_reason")
  private String rejectedReason;
}
