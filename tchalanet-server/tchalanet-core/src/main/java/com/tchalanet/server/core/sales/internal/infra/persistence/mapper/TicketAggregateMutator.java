package com.tchalanet.server.core.sales.internal.infra.persistence.mapper;

import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketChargeJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketLineJpaEntity;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketAggregateMutator {

    private final TicketJpaMapper mapper;

    public void applyTo(TicketJpaEntity managed, Ticket domain) {
        Objects.requireNonNull(managed, "managed ticket is required");
        Objects.requireNonNull(domain, "domain ticket is required");

        assertImmutableFields(managed, domain);

        mapper.applyMoney(domain.money(), managed);
        mapper.applyLifecycle(domain.lifecycle(), managed);
        mapper.applyPrintState(domain.print(), managed);

        applyLines(managed, domain);
        applyCharges(managed, domain);
    }

    private void assertImmutableFields(TicketJpaEntity managed, Ticket domain) {
        requireSame("ticketId", managed.getId(), domain.identity().id().value());
        requireSame("tenantId", managed.getTenantId(), domain.identity().tenantId().value());
        requireSame("outletId", managed.getOutletId(), domain.context().outletId().value());
        requireSame("terminalId", managed.getTerminalId(), domain.context().terminalId().value());
        requireSame("sellerUserId", managed.getSellerUserId(), domain.context().sellerUserId().value());
        requireSame("salesSessionId", managed.getSalesSessionId(), domain.context().salesSessionId().value());
        requireSame("drawId", managed.getDrawId(), domain.context().drawId().value());
        requireSame("drawChannelId", managed.getDrawChannelId(), domain.context().drawChannelId().value());
        requireSame("ticketCode", managed.getTicketCode(), domain.codes().ticketCode().value());
        requireSame("publicCode", managed.getPublicCode(), domain.codes().publicCode().value());
        requireSame("verificationCode", managed.getVerificationCode(), domain.codes().verificationCode().value());
        requireSame("saleChannel", managed.getSaleChannel(), domain.origin().channel());
        requireSame("currency", managed.getCurrency(), domain.money().currency().value());
        requireSame("stakeAmount", managed.getStakeAmount(), domain.money().breakdown().stake().amount());
        requireSame("totalAmount", managed.getTotalAmount(), domain.money().breakdown().total().amount());
        requireSame("potentialPayoutAmount", managed.getPotentialPayoutAmount(), domain.potentialPayout().amount());
    }

    private void applyLines(TicketJpaEntity managed, Ticket domain) {
        Map<UUID, TicketLineJpaEntity> existingById = new LinkedHashMap<>();
        for (var line : managed.getLines()) {
            existingById.put(line.getId(), line);
        }

        for (var domainLine : domain.lines()) {
            var existing = existingById.remove(domainLine.id().value());
            if (existing == null) {
                var created = mapper.toLineEntity(domainLine);
                created.setDrawId(domain.context().drawId().value());
                managed.addLine(created);
            } else {
                assertImmutableLineFields(existing, domainLine, domain);
                applyMutableLineFields(existing, domainLine);
            }
        }

        for (var orphan : existingById.values()) {
            managed.removeLine(orphan);
        }
    }

    private void assertImmutableLineFields(
        TicketLineJpaEntity managed,
        TicketLine domain,
        Ticket ticket
    ) {
        requireSame("line.drawId", managed.getDrawId(), ticket.context().drawId().value());
        requireSame("line.lineNumber", managed.getLineNumber(), domain.lineNumber());
        requireSame("line.gameCode", managed.getGameCode(), domain.gameCode());
        requireSame("line.betType", managed.getBetType(), domain.betType());
        requireSame("line.betOption", managed.getBetOption(), domain.betOption());
        requireSame("line.selectionKey", managed.getSelectionKey(), domain.selection().key().value());
        requireSame("line.displaySelection", managed.getDisplaySelection(), domain.selection().displayLabel());
        requireSame("line.stakeAmount", managed.getStakeAmount(), domain.stakeAmount().amount());
        requireSame("line.oddsSnapshot", managed.getOddsSnapshot(), domain.oddsSnapshot());
        requireSame(
            "line.potentialPayoutAmount",
            managed.getPotentialPayoutAmount(),
            domain.potentialPayoutAmount().amount());
    }

    private void applyMutableLineFields(TicketLineJpaEntity managed, TicketLine domain) {
        managed.setResultStatus(domain.resultStatus());
        managed.setPayoutAmount(domain.payoutAmount().amount());
    }

    private void applyCharges(TicketJpaEntity managed, Ticket domain) {
        Map<ChargeKey, TicketChargeJpaEntity> existingByKey = new LinkedHashMap<>();
        for (var charge : managed.getCharges()) {
            existingByKey.put(ChargeKey.of(charge), charge);
        }

        for (var domainCharge : domain.money().breakdown().charges()) {
            var key = ChargeKey.of(domainCharge);
            var existing = existingByKey.remove(key);
            if (existing == null) {
                managed.addCharge(mapper.toChargeEntity(domainCharge));
            } else {
                assertImmutableChargeFields(existing, domainCharge);
            }
        }

        for (var orphan : existingByKey.values()) {
            managed.removeCharge(orphan);
        }
    }

    private void assertImmutableChargeFields(TicketChargeJpaEntity managed, TicketCharge domain) {
        requireSame("charge.type", managed.getChargeType(), domain.type());
        requireSame("charge.paidBy", managed.getPaidBy(), domain.paidBy());
        requireSame("charge.amount", managed.getAmount(), domain.amount().amount());
        requireSame("charge.currency", managed.getCurrency(), domain.amount().currency().value());
    }

    private static void requireSame(String field, Object actual, Object expected) {
        if (actual instanceof BigDecimal a && expected instanceof BigDecimal e) {
            if (a.compareTo(e) == 0) {
                return;
            }
        }

        if (!Objects.equals(actual, expected)) {
            throw new IllegalStateException(
                "Ticket immutable field changed: "
                    + field
                    + " expected="
                    + actual
                    + " actual="
                    + expected);
        }
    }

    private record ChargeKey(
        Object type,
        Object paidBy
    ) {
        static ChargeKey of(TicketChargeJpaEntity entity) {
            return new ChargeKey(entity.getChargeType(), entity.getPaidBy());
        }

        static ChargeKey of(TicketCharge charge) {
            return new ChargeKey(charge.type(), charge.paidBy());
        }
    }
}
