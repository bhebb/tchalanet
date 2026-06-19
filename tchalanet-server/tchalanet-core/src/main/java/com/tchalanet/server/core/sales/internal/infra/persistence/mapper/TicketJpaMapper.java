package com.tchalanet.server.core.sales.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.lifecycle.ApprovalTrace;
import com.tchalanet.server.core.sales.api.model.lifecycle.DecisionTrace;
import com.tchalanet.server.core.sales.api.model.lifecycle.PaymentTrace;
import com.tchalanet.server.core.sales.api.model.lifecycle.ResultLifecycle;
import com.tchalanet.server.core.sales.api.model.lifecycle.SaleLifecycle;
import com.tchalanet.server.core.sales.api.model.lifecycle.SettlementLifecycle;
import com.tchalanet.server.core.sales.api.model.lifecycle.TicketLifecycle;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.api.model.money.TicketMoney;
import com.tchalanet.server.core.sales.api.model.money.TicketMoneyBreakdown;
import com.tchalanet.server.core.sales.api.model.origin.TicketOrigin;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintState;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.value.PublicCode;
import com.tchalanet.server.core.sales.api.model.value.TicketCode;
import com.tchalanet.server.core.sales.api.model.value.VerificationCode;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketAudit;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketCodes;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketContext;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketIdentity;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketChargeJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketLineJpaEntity;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionKey;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TicketJpaMapper {

    // ---------------------------------------------------------------------------
    // Entity -> Domain aggregate
    // ---------------------------------------------------------------------------

    default Ticket toDomain(TicketJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        var currency = CurrencyCode.of(entity.getCurrency());


        return new Ticket(
            toIdentity(entity),
            toContext(entity),
            toCodes(entity),
            toMoney(entity),
            toLifecycle(entity),
            toOrigin(entity),
            toPrintState(entity),
            toAudit(entity),
            toDomainLines(entity.getLines(), currency)
        );
    }

    // ---------------------------------------------------------------------------
    // Domain aggregate -> Entity
    // ---------------------------------------------------------------------------

    default TicketJpaEntity toEntity(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        var entity = new TicketJpaEntity();
        // BaseEntity id
        entity.setId(ticket.identity().id().value());
        entity.setTenantId(ticket.identity().tenantId().value());

        applyIdentity(ticket.identity(), entity);
        applyContext(ticket.context(), entity);
        applyCodes(ticket.codes(), entity);
        applyMoney(ticket.money(), entity);
        applyLifecycle(ticket.lifecycle(), entity);
        applyOrigin(ticket.origin(), entity);
        applyPrintState(ticket.print(), entity);

        var lineEntities = ticket.lines().stream()
            .map(this::toLineEntity)
            .peek(line -> {
                line.setDrawId(ticket.context().drawId().value());
            })
            .toList();
        entity.replaceLines(lineEntities);

        var chargeEntities = ticket.money().breakdown().charges().stream()
            .map(this::toChargeEntity)
            .toList();
        entity.replaceCharges(chargeEntities);

        return entity;
    }

    default TicketLineJpaEntity toLineEntity(TicketLine line) {
        var entity = new TicketLineJpaEntity();

        entity.setId(line.id().value());
        entity.setLineNumber(line.lineNumber());
        entity.setGameCode(line.gameCode());
        entity.setBetType(line.betType());
        entity.setSelectionKey(line.selection().key().value());
        entity.setOddsSnapshot(line.oddsSnapshot());
        entity.setDisplaySelection(line.selection().displayLabel());
        entity.setStakeAmount(line.stakeAmount().amount());
        entity.setPayoutBaseAmount(line.payoutBaseAmount().amount());
        entity.setBetOption(line.betOption());
        entity.setPotentialPayoutAmount(line.potentialPayoutAmount().amount());
        entity.setResultStatus(line.resultStatus());
        entity.setPayoutAmount(line.payoutAmount().amount());
        entity.setOrigin(line.origin());
        entity.setPricingSource(line.pricingSource());
        entity.setSelectionSource(line.selectionSource());
        entity.setPromotionDecisionId(line.promotionDecisionId() == null ? null : line.promotionDecisionId().value());
        entity.setPromotionLabel(line.promotionLabel());
        entity.setPromotionEffectType(line.promotionEffectType());

        return entity;
    }

    default List<TicketChargeJpaEntity> toChargeEntities(List<TicketCharge> charges) {
        if (charges == null) {
            return List.of();
        }

        return charges.stream()
            .map(this::toChargeEntity)
            .toList();
    }

    default TicketChargeJpaEntity toChargeEntity(TicketCharge charge) {
        var entity = new TicketChargeJpaEntity();

        entity.setChargeType(charge.type());
        entity.setPaidBy(charge.paidBy());
        entity.setAmount(charge.amount().amount());
        entity.setCurrency(charge.amount().currency().value());
        entity.setWaivedByRuleId(charge.waivedByRuleId() == null ? null : charge.waivedByRuleId().value());
        entity.setWaivedByDecisionId(charge.waivedByDecisionId() == null ? null : charge.waivedByDecisionId().value());
        entity.setWaivedEffectType(charge.waivedEffectType());
        entity.setWaivedLabel(charge.waivedLabel());

        return entity;
    }

    // ---------------------------------------------------------------------------
    // Entity -> sub-objects
    // ---------------------------------------------------------------------------

    default TicketIdentity toIdentity(TicketJpaEntity entity) {
        return new TicketIdentity(
            TicketId.of(entity.getId()),
            TenantId.of(entity.getTenantId())
        );
    }

    default TicketContext toContext(TicketJpaEntity entity) {
        return new TicketContext(
            DrawId.of(entity.getDrawId()),
            DrawChannelId.of(entity.getDrawChannelId()),
            SellerTerminalId.nullableOf(entity.getSellerTerminalId()),
            entity.getSellerCommissionRateSnapshot(),
            entity.getSellerCommissionAmountSnapshot()
        );
    }

    default TicketCodes toCodes(TicketJpaEntity entity) {
        return new TicketCodes(
            TicketCode.of(entity.getTicketCode()),
            PublicCode.of(entity.getPublicCode()),
            VerificationCode.of(entity.getVerificationCode())
        );
    }

    default TicketMoney toMoney(TicketJpaEntity entity) {
        var currency = CurrencyCode.of(entity.getCurrency());

        return new TicketMoney(currency,
            new TicketMoneyBreakdown(
                new Money(entity.getStakeAmount(), currency),
                toDomainCharges(entity.getCharges(), currency),
                new Money(entity.getTotalAmount(), currency)
            ),
            new Money(entity.getPotentialPayoutAmount(), currency)
        );
    }

    default TicketLifecycle toLifecycle(TicketJpaEntity entity) {
        return new TicketLifecycle(
            toSaleLifecycle(entity),
            toResultLifecycle(entity),
            toSettlementLifecycle(entity)
        );
    }

    default SaleLifecycle toSaleLifecycle(TicketJpaEntity entity) {
        return new SaleLifecycle(
            entity.getSaleStatus(),
            entity.getSoldAt(),
            entity.getPlacedAt(),
            toApprovalTrace(entity),
            toRejectedTrace(entity),
            toCancelledTrace(entity),
            toVoidedTrace(entity)
        );
    }

    default ResultLifecycle toResultLifecycle(TicketJpaEntity entity) {
        var currency = CurrencyCode.of(entity.getCurrency());

        return new ResultLifecycle(
            entity.getResultStatus(),
            new Money(entity.getWinningAmount(), currency),
            entity.getResultedAt(),
            nullableUserId(entity.getResultedBy()),
            entity.getResultOverrideReason()
        );
    }

    default SettlementLifecycle toSettlementLifecycle(TicketJpaEntity entity) {
        return new SettlementLifecycle(
            entity.getSettlementStatus(),
            entity.getSettledAt(),
            nullableUserId(entity.getSettledBy()),
            toPaymentTrace(entity)
        );
    }

    default PaymentTrace toPaymentTrace(TicketJpaEntity entity) {
        return null;
    }

    default ApprovalTrace toApprovalTrace(TicketJpaEntity entity) {
        if (entity.getApprovalRequestId() == null) {
            return null;
        }

        return new ApprovalTrace(
            ApprovalRequestId.of(entity.getApprovalRequestId()),
            nullableUserId(entity.getApprovalRequestedBy()),
            entity.getApprovalRequestedAt(),
            entity.getApprovedAt(),
            nullableUserId(entity.getApprovedBy())
        );
    }

    default DecisionTrace toRejectedTrace(TicketJpaEntity entity) {
        if (entity.getRejectedAt() == null && entity.getRejectedBy() == null) {
            return null;
        }

        return new DecisionTrace(
            entity.getRejectedAt(),
            nullableUserId(entity.getRejectedBy()),
            entity.getRejectionReason()
        );
    }

    default DecisionTrace toCancelledTrace(TicketJpaEntity entity) {
        if (entity.getCancelledAt() == null && entity.getCancelledBy() == null) {
            return null;
        }

        return new DecisionTrace(
            entity.getCancelledAt(),
            nullableUserId(entity.getCancelledBy()),
            entity.getCancellationReason()
        );
    }

    default DecisionTrace toVoidedTrace(TicketJpaEntity entity) {
        if (entity.getVoidedAt() == null && entity.getVoidedBy() == null) {
            return null;
        }

        return new DecisionTrace(
            entity.getVoidedAt(),
            nullableUserId(entity.getVoidedBy()),
            entity.getVoidReason()
        );
    }

    default TicketOrigin toOrigin(TicketJpaEntity entity) {
        return new TicketOrigin(
            entity.getSaleChannel());
    }


    default TicketPrintState toPrintState(TicketJpaEntity entity) {
        return new TicketPrintState(
            entity.getPrintStatus(),
            entity.getPrintCount(),
            entity.getFirstPrintedAt(),
            entity.getLastPrintedAt()
        );
    }

    default TicketAudit toAudit(TicketJpaEntity entity) {
        return new TicketAudit(
            entity.getCreatedAt(),
            nullableUserId(entity.getCreatedBy()),
            entity.getUpdatedAt(),
            nullableUserId(entity.getUpdatedBy())
        );
    }

    default List<TicketLine> toDomainLines(
        List<TicketLineJpaEntity> entities,
        @Context CurrencyCode currency
    ) {
        if (entities == null) return List.of();

        return entities.stream()
            .map(line -> toDomainLine(line, currency))
            .toList();
    }

    default TicketLine toDomainLine(TicketLineJpaEntity entity, @Context CurrencyCode currency) {
        return new TicketLine(
            TicketLineId.of(entity.getId()),
            entity.getLineNumber(),
            entity.getGameCode(),
            entity.getBetType(),
            new Selection(
                SelectionKey.of(entity.getSelectionKey()),
                entity.getDisplaySelection()
            ),
            new Money(entity.getStakeAmount(), currency),
            new Money(entity.getPayoutBaseAmount(), currency),
            entity.getOddsSnapshot(),
            new Money(entity.getPotentialPayoutAmount(), currency),
            entity.getBetOption(),
            entity.getOrigin() == null ? TicketLineOrigin.CUSTOMER : entity.getOrigin(),
            entity.getPricingSource() == null ? TicketLinePricingSource.STANDARD : entity.getPricingSource(),
            entity.getSelectionSource() == null ? TicketLineSelectionSource.CUSTOMER_SELECTED : entity.getSelectionSource(),
            entity.getPromotionDecisionId() == null ? null : PromotionDecisionId.of(entity.getPromotionDecisionId()),
            entity.getPromotionLabel(),
            entity.getPromotionEffectType(),
            entity.getResultStatus(),
            new Money(entity.getPayoutAmount(), currency)
        );
    }

    // ---------------------------------------------------------------------------
    // Domain sub-objects -> Entity flattening
    // ---------------------------------------------------------------------------

    default void applyIdentity(TicketIdentity identity, @MappingTarget TicketJpaEntity entity) {
        // id and tenant_id are set directly in toEntity(...); listener auto-fills on insert and
        // verifies on update.
    }

    default void applyContext(TicketContext context, @MappingTarget TicketJpaEntity entity) {
        entity.setDrawId(context.drawId().value());
        entity.setDrawChannelId(context.drawChannelId().value());
        entity.setSellerTerminalId(context.sellerTerminalId() == null ? null : context.sellerTerminalId().value());
        entity.setSellerCommissionRateSnapshot(context.sellerCommissionRateSnapshot());
        entity.setSellerCommissionAmountSnapshot(context.sellerCommissionAmountSnapshot());
    }

    default void applyCodes(TicketCodes codes, @MappingTarget TicketJpaEntity entity) {
        entity.setTicketCode(codes.ticketCode().value());
        entity.setPublicCode(codes.publicCode().value());
        entity.setVerificationCode(codes.verificationCode().value());
    }

    default void applyMoney(TicketMoney money, @MappingTarget TicketJpaEntity entity) {
        entity.setCurrency(money.breakdown().total().currency().value());
        entity.setStakeAmount(money.breakdown().stake().amount());
        entity.setTotalAmount(money.breakdown().total().amount());
        entity.setPotentialPayoutAmount(money.potentialPayoutAmount().amount());
    }

    default void applyLifecycle(TicketLifecycle lifecycle, @MappingTarget TicketJpaEntity entity) {
        applySaleLifecycle(lifecycle.sale(), entity);
        applyResultLifecycle(lifecycle.result(), entity);
        applySettlementLifecycle(lifecycle.settlement(), entity);
    }

    default void applySaleLifecycle(SaleLifecycle sale, @MappingTarget TicketJpaEntity entity) {
        entity.setSaleStatus(sale.status());
        entity.setSoldAt(sale.soldAt());
        entity.setPlacedAt(sale.placedAt());

        applyApprovalTrace(sale.approval(), entity);
        applyRejectedTrace(sale.rejection(), entity);
        applyCancelledTrace(sale.cancellation(), entity);
        applyVoidedTrace(sale.voiding(), entity);
    }

    default void applyApprovalTrace(ApprovalTrace approval, @MappingTarget TicketJpaEntity entity) {
        if (approval == null) {
            entity.setApprovalRequestId(null);
            entity.setApprovalRequestedBy(null);
            entity.setApprovalRequestedAt(null);
            entity.setApprovedAt(null);
            entity.setApprovedBy(null);
            return;
        }

        entity.setApprovalRequestId(approval.requestId().value());
        entity.setApprovalRequestedBy(value(approval.requestedBy()));
        entity.setApprovalRequestedAt(approval.requestedAt());
        entity.setApprovedAt(approval.approvedAt());
        entity.setApprovedBy(value(approval.approvedBy()));
    }

    default void applyRejectedTrace(DecisionTrace trace, @MappingTarget TicketJpaEntity entity) {
        entity.setRejectedBy(trace == null ? null : value(trace.by()));
        entity.setRejectedAt(trace == null ? null : trace.at());
        entity.setRejectionReason(trace == null ? null : trace.reason());
    }

    default void applyCancelledTrace(DecisionTrace trace, @MappingTarget TicketJpaEntity entity) {
        entity.setCancelledBy(trace == null ? null : value(trace.by()));
        entity.setCancelledAt(trace == null ? null : trace.at());
        entity.setCancellationReason(trace == null ? null : trace.reason());
    }

    default void applyVoidedTrace(DecisionTrace trace, @MappingTarget TicketJpaEntity entity) {
        entity.setVoidedBy(trace == null ? null : value(trace.by()));
        entity.setVoidedAt(trace == null ? null : trace.at());
        entity.setVoidReason(trace == null ? null : trace.reason());
    }

    default void applyResultLifecycle(ResultLifecycle result, @MappingTarget TicketJpaEntity entity) {
        entity.setResultStatus(result.status());
        entity.setWinningAmount(result.winningAmount().amount());
        entity.setResultedAt(result.resultedAt());
        entity.setResultedBy(value(result.resultedBy()));
        entity.setResultOverrideReason(result.overrideReason());
    }

    default void applySettlementLifecycle(
        SettlementLifecycle settlement,
        @MappingTarget TicketJpaEntity entity
    ) {
        entity.setSettlementStatus(settlement.status());
        entity.setSettledAt(settlement.settledAt());
        entity.setSettledBy(value(settlement.settledBy()));

        if (settlement.payment() == null) {
            entity.setPaidAt(null);
            entity.setPaidBy(null);
            return;
        }

        entity.setPaidAt(settlement.payment().paidAt());
        entity.setPaidBy(value(settlement.payment().paidBy()));
    }

    default void applyOrigin(TicketOrigin origin, @MappingTarget TicketJpaEntity entity) {
        entity.setSaleChannel(origin.channel());
    }

    default void applyPrintState(TicketPrintState print, @MappingTarget TicketJpaEntity entity) {
        entity.setPrintStatus(print.status());
        entity.setPrintCount(print.printCount());
        entity.setFirstPrintedAt(print.firstPrintedAt());
        entity.setLastPrintedAt(print.lastPrintedAt());
    }

    default List<TicketCharge> toDomainCharges(
        List<TicketChargeJpaEntity> entities,
        CurrencyCode currency
    ) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
            .map(entity -> toDomainCharge(entity, currency))
            .toList();
    }

    default TicketCharge toDomainCharge(
        TicketChargeJpaEntity entity,
        CurrencyCode currency
    ) {
        return new TicketCharge(
            entity.getChargeType(),
            new Money(entity.getAmount(), currency),
            entity.getPaidBy(),
            entity.getWaivedByDecisionId() == null ? null : PromotionDecisionId.of(entity.getWaivedByDecisionId()),
            entity.getWaivedByRuleId() == null ? null : PromotionRuleId.of(entity.getWaivedByRuleId()),
            entity.getWaivedEffectType(),
            entity.getWaivedLabel()
        );
    }

    // ---------------------------------------------------------------------------
    // small value helpers
    // ---------------------------------------------------------------------------

    default UserId nullableUserId(UUID value) {
        return value == null ? null : UserId.of(value);
    }

    default UUID value(UserId id) {
        return id == null ? null : id.value();
    }
}
