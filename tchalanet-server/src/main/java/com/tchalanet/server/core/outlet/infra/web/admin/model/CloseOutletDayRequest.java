package com.tchalanet.server.core.outlet.infra.web.admin.model;

import com.tchalanet.server.core.outlet.application.command.model.CloseDayMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CloseOutletDayRequest(
    LocalDate from, LocalDate to, CloseDayMode mode, @Size(max = 255) String reason) {
  public static CloseOutletDayRequest empty() {
    return new CloseOutletDayRequest(null, null, null, null);
  }

  public LocalDate fromOrNow() {
    return from == null ? LocalDate.now() : from;
  }

  public LocalDate toOrNow() {
    return to == null ? LocalDate.now() : to;
  }

  public CloseDayMode modeOrDefault() {
    return mode == null ? CloseDayMode.STRICT : mode;
  }

  @AssertTrue(message = "'from' must be on or before 'to'")
  public boolean isFromBeforeOrEqualToTo() {
    if (from == null || to == null) return true;
    return !from.isAfter(to);
  }
}
