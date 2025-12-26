package com.tchalanet.server.core.session.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.port.out.PosSessionReaderPort;
import com.tchalanet.server.core.session.application.port.out.PosSessionTotalsReaderPort;
import com.tchalanet.server.core.session.application.query.model.GetSessionWithTotalsQuery;
import com.tchalanet.server.core.session.application.query.model.SessionWithTotalsDto;

import java.math.BigDecimal;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSessionWithTotalsQueryHandler implements QueryHandler<GetSessionWithTotalsQuery, Optional<SessionWithTotalsDto>> {

  private final PosSessionReaderPort sessionReader;
  private final PosSessionTotalsReaderPort totalsReader;

  @Override
  public Optional<SessionWithTotalsDto> handle(GetSessionWithTotalsQuery query) {
    var sessionOpt = sessionReader.findById(query.sessionId().value());
    if (sessionOpt.isEmpty()) {
      return Optional.empty();
    }
    var session = sessionOpt.get();
    var totalsOpt = totalsReader.findBySessionId(query.sessionId());

    var totalsDto = totalsOpt.map(t -> new SessionWithTotalsDto.PosSessionTotalsDto(
        t.totalTickets(),
        t.totalStake(),
        t.totalPayout(),
        t.grossMargin(),
        t.updatedAt()
    )).orElse(null);

    var dto = new SessionWithTotalsDto(
        session.id(),
        session.tenantId(),
        session.outletId(),
        session.terminalId(),
        session.userId(),
        session.status().name(),
        session.openedAt(),
        session.closedAt(),
        session.openingFloatCents() != null ? BigDecimal.valueOf(session.openingFloatCents()).movePointLeft(2) : null,
        session.closingAmountCents() != null ? BigDecimal.valueOf(session.closingAmountCents()).movePointLeft(2) : null,
        totalsDto
    );

    return Optional.of(dto);
  }
}
