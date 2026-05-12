package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.util.List;

public record CancelSaleResult(
    Ticket ticket,
    CancelOutcome outcome,
    List<LimitNotice> warnings
) {
  public enum CancelOutcome {
    SUCCESS,
    SUCCESS_WITH_WARNINGS
  }
}
