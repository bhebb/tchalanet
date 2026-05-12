package com.tchalanet.server.core.sales.internal.application.service;

import com.tchalanet.server.core.sales.api.command.SellTicketLineInput;
import com.tchalanet.server.core.sales.internal.domain.model.TicketMoneyBreakdown;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TicketMoneyCalculator {

  public TicketMoneyBreakdown calculate(List<SellTicketLineInput> lines, BigDecimal feeAmount) {
    BigDecimal stake = lines.stream()
        .map(SellTicketLineInput::stakeAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new TicketMoneyBreakdown(stake, feeAmount == null ? BigDecimal.ZERO : feeAmount, stake.add(feeAmount == null ? BigDecimal.ZERO : feeAmount));
  }
}
