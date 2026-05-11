package com.tchalanet.server.core.sales.infra.persistence;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.sales.domain.model.*;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = CommonIdMapper.class)
public interface TicketJpaMapper {

  default Ticket toDomain(TicketJpaEntity e, List<TicketLineJpaEntity> lineEntities) {
    var money = new TicketMoneyBreakdown(e.getStakeAmount(), e.getFeeAmount(), e.getTotalAmount());

    OfflineSaleRef offlineRef = null;
    if ("OFFLINE".equals(e.getSaleOrigin())) {
      offlineRef = new OfflineSaleRef(
          OfflineSaleSubmissionId.nullableOf(e.getOfflineSubmissionId()),
          OfflineBatchId.nullableOf(e.getOfflineBatchId()),
          OfflineCodeBatchId.nullableOf(e.getOfflineCodeBatchId()),
          e.getOfflineCode(),
          e.getClientTicketId(),
          e.getLocalSequence() == null ? 0 : e.getLocalSequence(),
          e.getCreatedAtDevice());
    }

    return new Ticket(
        TicketId.of(e.getId()),
        TenantId.of(e.getTenantId()),
        OutletId.of(e.getOutletId()),
        TerminalId.of(e.getTerminalId()),
        UserId.of(e.getSellerUserId()),
        SalesSessionId.of(e.getSalesSessionId()),
        DrawId.of(e.getDrawId()),
        DrawChannelId.nullableOf(e.getDrawChannelId()),
        e.getTicketCode(),
        e.getPublicCode(),
        e.getVerificationCode(),
        CurrencyCode.of(e.getCurrency()),
        money,
        e.getPotentialPayoutAmount(),
        e.getWinningAmount(),
        TicketSaleStatus.valueOf(e.getSaleStatus()),
        TicketResultStatus.valueOf(e.getResultStatus()),
        TicketSettlementStatus.valueOf(e.getSettlementStatus()),
        SaleOrigin.valueOf(e.getSaleOrigin()),
        TicketSyncStatus.valueOf(e.getSyncStatus()),
        offlineRef,
        e.getOfflineSubmissionId() == null ? SalesSessionPostingMode.NORMAL_OPEN_SESSION : SalesSessionPostingMode.NORMAL_OPEN_SESSION,
        e.getSoldAt(),
        null,
        null,
        e.getPaidAt(),
        UserId.nullableOf(e.getPaidBy()),
        lineEntities.stream().map(this::toLineDomain).toList());
  }

  default TicketLine toLineDomain(TicketLineJpaEntity e) {
    return new TicketLine(
        e.getLineNo(),
        e.getGameCode(),
        e.getSelection(),
        e.getBetType(),
        e.getBetOption(),
        e.getStakeAmount(),
        e.getOddsSnapshot(),
        e.getPotentialPayoutAmount());
  }

  default TicketJpaEntity toEntity(Ticket ticket) {
    var e = new TicketJpaEntity();
    e.setId(ticket.id().value());
    e.setTenantId(ticket.tenantId().value());
    e.setOutletId(ticket.outletId().value());
    e.setTerminalId(ticket.terminalId().value());
    e.setSellerUserId(ticket.sellerUserId().value());
    e.setSalesSessionId(ticket.salesSessionId().value());
    e.setDrawId(ticket.drawId().value());
    e.setDrawChannelId(ticket.drawChannelId() == null ? null : ticket.drawChannelId().value());
    e.setTicketCode(ticket.ticketCode());
    e.setPublicCode(ticket.publicCode());
    e.setVerificationCode(ticket.verificationCode());
    e.setCurrency(ticket.currency().value());
    e.setStakeAmount(ticket.money().stakeAmount());
    e.setFeeAmount(ticket.money().feeAmount());
    e.setTotalAmount(ticket.money().totalAmount());
    e.setPotentialPayoutAmount(ticket.potentialPayoutAmount());
    e.setWinningAmount(ticket.winningAmount());
    e.setSaleStatus(ticket.saleStatus().name());
    e.setResultStatus(ticket.resultStatus().name());
    e.setSettlementStatus(ticket.settlementStatus().name());
    e.setSaleOrigin(ticket.saleOrigin().name());
    e.setSyncStatus(ticket.syncStatus().name());
    if (ticket.offlineSaleRef() != null) {
      e.setOfflineSubmissionId(ticket.offlineSaleRef().submissionId().value());
      e.setOfflineBatchId(ticket.offlineSaleRef().batchId().value());
      e.setOfflineCodeBatchId(ticket.offlineSaleRef().codeBatchId().value());
      e.setOfflineCode(ticket.offlineSaleRef().offlineCode());
      e.setClientTicketId(ticket.offlineSaleRef().clientTicketId());
      e.setLocalSequence(ticket.offlineSaleRef().localSequence());
      e.setCreatedAtDevice(ticket.offlineSaleRef().createdAtDevice());
      e.setSyncedAt(ticket.soldAt());
    }
    e.setSoldAt(ticket.soldAt());
    e.setPaidAt(ticket.paidAt());
    e.setPaidBy(ticket.paidBy() == null ? null : ticket.paidBy().value());
    return e;
  }

  default TicketLineJpaEntity toLineEntity(Ticket ticket, TicketLine line) {
    var e = new TicketLineJpaEntity();
    e.setId(java.util.UUID.randomUUID()); // TODO replace with IdGenerator in adapter if strict.
    e.setTenantId(ticket.tenantId().value());
    e.setTicketId(ticket.id().value());
    e.setLineNo(line.lineNo());
    e.setGameCode(line.gameCode());
    e.setSelection(line.selection());
    e.setBetType(line.betType());
    e.setBetOption(line.betOption());
    e.setStakeAmount(line.stakeAmount());
    e.setOddsSnapshot(line.oddsSnapshot());
    e.setPotentialPayoutAmount(line.potentialPayoutAmount());
    e.setResultStatus("NOT_RESULTED");
    return e;
  }
}
