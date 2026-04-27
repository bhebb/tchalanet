package com.tchalanet.server.core.sales.infra.persistence.mapper;

import com.tchalanet.server.common.selection.SelectionKeyCanonicalizer;
import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.enums.TicketSettlementStatus;
import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import com.tchalanet.server.core.sales.infra.persistence.TicketLineEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketMapper {

    public Ticket toDomain(TicketEntity e) {
        List<TicketLine> lines =
            e.getLines() == null ? List.of() : e.getLines().stream().map(this::toDomainLine).toList();

        return Ticket.rehydrate(
            TicketId.of(e.getId()),
            TenantId.of(e.getTenantId()),
            TerminalId.of(e.getTerminalId()),
            e.getSessionId() == null ? null : SessionId.of(e.getSessionId()),
            DrawId.of(e.getDrawId()),
            e.getTicketCode(),
            e.getPublicCode(),
            e.getCurrency(),
            // ✅ split statuses
            e.getSaleStatus(),
            e.getResultStatus(),
            e.getSettlementStatus(),
            e.getTotalAmount(),
            e.getWinningAmount(),
            e.getResultedAt(),
            lines,
            e.getApprovalRequestId(),
            e.getCreatedAt(),
            e.getUpdatedAt());
    }

    private TicketLine toDomainLine(TicketLineEntity le) {
        // convert persisted String gameCode -> enum GameCode
        com.tchalanet.server.common.types.enums.GameCode gc;
        try {
            gc = com.tchalanet.server.common.types.enums.GameCode.valueOf(le.getGameCode());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unknown gameCode in DB: " + le.getGameCode(), ex);
        }

        return new TicketLine(
            gc,
            le.getSelection(),
            le.getStake(),
            le.getOddsSnapshot(),
            le.getPotentialPayout(),
            le.getBetType(),
            le.getBetOption());
    }

    public TicketEntity toEntity(Ticket domain) {
        TicketEntity e = new TicketEntity();
        e.setId(domain.getId().value());
        e.setTenantId(domain.getTenantId().value());
        e.setTerminalId(domain.getTerminalId().value());
        e.setSessionId(domain.getSessionId() == null ? null : domain.getSessionId().value());
        e.setDrawId(domain.getDrawId().value());

        e.setTicketCode(domain.getTicketCode());
        e.setPublicCode(domain.getPublicCode());
        e.setCurrency(domain.getCurrency());

        // ✅ split statuses mapped 1:1
        e.setSaleStatus(domain.getSaleStatus());
        e.setResultStatus(domain.getResultStatus());
        e.setSettlementStatus(domain.getSettlementStatus());

        e.setTotalAmount(domain.getTotalAmount());
        e.setWinningAmount(domain.getWinningAmount());
        e.setResultedAt(domain.getResultedAt());
        e.setApprovalRequestId(domain.getApprovalRequestId());

        var lineEntities = domain.getLines().stream().map(this::toEntityLine).toList();
        e.clearAndAddLines(lineEntities);
        return e;
    }

    private TicketLineEntity toEntityLine(TicketLine line) {
        TicketLineEntity le = new TicketLineEntity();
        // persist enum as its name/string representation
        le.setGameCode(line.gameCode().name());
        // ensure persisted selection is canonical
        le.setSelection(SelectionKeyCanonicalizer.canonicalize(line.betType(), line.selection()));
        le.setStake(line.stake());
        le.setOddsSnapshot(line.oddsSnapshot());
        le.setPotentialPayout(line.potentialPayout());
        le.setBetType(line.betType());
        le.setBetOption(line.betOption()); // ✅ new
        return le;
    }

    // Optional defensive defaults if entity has nulls (older rows / migrations)
    private static TicketSaleStatus safeSale(TicketSaleStatus s) {
        return s == null ? TicketSaleStatus.SOLD : s;
    }

    private static TicketResultStatus safeResult(TicketResultStatus s) {
        return s == null ? TicketResultStatus.NOT_RESULTED : s;
    }

    private static TicketSettlementStatus safeSettlement(TicketSettlementStatus s) {
        return s == null ? TicketSettlementStatus.UNSETTLED : s;
    }
}
