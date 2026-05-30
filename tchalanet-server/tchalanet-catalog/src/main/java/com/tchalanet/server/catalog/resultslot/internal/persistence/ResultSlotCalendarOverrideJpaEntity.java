package com.tchalanet.server.catalog.resultslot.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Global provider calendar override for a result_slot (no tenant scope).
 *
 * <p>Not {@code @Audited} — there is no {@code result_slot_calendar_override_aud}
 * table; attribution is via {@code created_by} / {@code updated_by} on
 * {@link BaseEntity}.
 *
 * <p>XOR shape (DB CHECK {@code chk_result_slot_calendar_override__shape}):
 * exactly one of {@link #slotLocalDate} / {@link #recurringMd} is set.
 */
@Entity
@Table(name = "result_slot_calendar_override")
@Getter
@Setter
public class ResultSlotCalendarOverrideJpaEntity extends BaseEntity {

  @Column(name = "result_slot_id", nullable = false)
  private UUID resultSlotId;

  /** Specific dated occurrence (slot timezone). Null when {@link #recurringMd} is set. */
  @Column(name = "slot_local_date")
  private LocalDate slotLocalDate;

  /** Year-less 'MM-dd' annual rule. Null when {@link #slotLocalDate} is set. */
  @Column(name = "recurring_md", length = 5)
  private String recurringMd;

  @Column(name = "available", nullable = false)
  private boolean available;

  @Column(name = "reason_code", nullable = false, length = 96)
  private String reasonCode;

  @Column(name = "reason_label", length = 255)
  private String reasonLabel;
}
