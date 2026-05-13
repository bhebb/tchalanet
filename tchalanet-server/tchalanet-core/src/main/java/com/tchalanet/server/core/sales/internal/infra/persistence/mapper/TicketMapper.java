package com.tchalanet.server.core.sales.internal.infra.persistence.mapper;

import com.tchalanet.server.common.selection.SelectionKeyCanonicalizer;
import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.enums.TicketSettlementStatus;
import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.sales.internal.domain.model.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.TicketLine;
import com.tchalanet.server.core.sales.internal.infra.persistence.TicketJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.TicketLineJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketMapper {

    public Ticket toDomain(TicketJpaEntity entity) {
        List<TicketLine> lines =
            entity.getLines() == null ? List.of() : entity.getLines().stream().map(this::toDomainLine).toList();

        return Ticket.rehydrate(
            TicketId.of(entity.getId()),
            TenantId.of(entity.getTenantId()),
            TerminalId.of(entity.getTerminalId()),
            entity.getSalesSessionId() == null ? null : SalesSessionId.of(entity.getSalesSessionId()),
            DrawId.of(entity.getDrawId()),
            entity.getTicketCode(),
            entity.getPublicCode(),
            entity.getCurrency(),
            // ✅ split statuses
            entity.getSaleStatus(),
            entity.getResultStatus(),
            entity.getSettlementStatus(),
            entity.getTotalAmount(),
            entity.getWinningAmount(),
            entity.getResultedAt(),
            lines,
            ApprovalRequestId.nullableOf(entity.getApprovalRequestId()),
            entity.getCreatedAt(),
            entity.getUpdatedAt());
    }

    private TicketLine toDomainLine(TicketLineJpaEntity lineEntity) {
        // convert persisted String externalGameCode -> enum GameCode
        com.tchalanet.server.common.types.enums.GameCode gameCode;
        try {
            gameCode = com.tchalanet.server.common.types.enums.GameCode.valueOf(lineEntity.getGameCode());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unknown externalGameCode in DB: " + lineEntity.getGameCode(), ex);
        }

        return new TicketLine(
            gameCode,
            lineEntity.getSelection(),
            lineEntity.getStake(),
            lineEntity.getOddsSnapshot(),
            lineEntity.getPotentialPayout(),
            lineEntity.getBetType(),
            lineEntity.getBetOption());
    }

    public TicketJpaEntity toEntity(Ticket domain) {
        TicketJpaEntity entity = new TicketJpaEntity();
        entity.setId(domain.getId().value());
        entity.setTenantId(domain.getTenantId().value());
        entity.setTerminalId(domain.getTerminalId().value());
        entity.setSalesSessionId(domain.getSalesSessionId() == null ? null : domain.getSalesSessionId().value());
        entity.setDrawId(domain.getDrawId().value());

        entity.setTicketCode(domain.getTicketCode());
        entity.setPublicCode(domain.getPublicCode());
        entity.setCurrency(domain.getCurrency().code());

        // ✅ split statuses mapped 1:1
        entity.setSaleStatus(domain.getSaleStatus());
        entity.setResultStatus(domain.getResultStatus());
        entity.setSettlementStatus(domain.getSettlementStatus());

        entity.setTotalAmount(domain.getMoney().totalAmount());
        entity.setWinningAmount(domain.getWinningAmount());
        entity.setResultedAt(domain.getResultedAt());
        entity.setApprovalRequestId(
            domain.getApprovalRequestId() == null ? null : domain.getApprovalRequestId().value());

        entity.setOutletId(domain.getOutletId().value());
        entity.setSellerUserId(domain.getSellerUserId().value());
        entity.setVerificationCode(domain.getVerificationCode());
        entity.setStakeAmount(domain.getMoney().stakeAmount());
        entity.setFeeAmount(domain.getMoney().feeAmount());
        entity.setPotentialPayoutAmount(domain.getPotentialPayoutAmount());
        entity.setSaleOrigin(domain.getSaleOrigin().name());
        entity.setSyncStatus(domain.getSyncStatus().name());
        entity.setSoldAt(domain.getSoldAt());
        entity.setPaidAt(domain.getPaidAt());
        entity.setPaidBy(domain.getPaidBy() == null ? null : domain.getPaidBy().value());

        var lineEntities = domain.getLines().stream().map(this::toEntityLine).toList();
        entity.clearAndAddLines(lineEntities);
        return entity;
    }

    private TicketLineJpaEntity toEntityLine(TicketLine line) {
        TicketLineJpaEntity lineEntity = new TicketLineJpaEntity();
        // persist enum as its name/string representation
        lineEntity.setGameCode(line.gameCode().name());
        // ensure persisted selection is canonical
        lineEntity.setSelection(SelectionKeyCanonicalizer.canonicalize(line.betType(), line.selection()));
        lineEntity.setStake(line.stakeAmount());
        lineEntity.setOddsSnapshot(line.oddsSnapshot());
        lineEntity.setPotentialPayout(line.potentialPayoutAmount());
        lineEntity.setBetType(line.betType());
        lineEntity.setBetOption(line.betOption());
        return lineEntity;
    }

    // Optional defensive defaults if entity has nulls (older rows / migrations)
    private static TicketSaleStatus safeSale(TicketSaleStatus status) {
        return status == null ? TicketSaleStatus.SOLD : status;
    }

    private static TicketResultStatus safeResult(TicketResultStatus status) {
        return status == null ? TicketResultStatus.NOT_RESULTED : status;
    }

    private static TicketSettlementStatus safeSettlement(TicketSettlementStatus status) {
        return status == null ? TicketSettlementStatus.UNSETTLED : status;
    }
}
