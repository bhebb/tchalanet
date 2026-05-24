package com.tchalanet.server.core.sales.internal.application.service.offline;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.query.GetDrawByIdQuery;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionLineSnapshot;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionTicketDraft;
import com.tchalanet.server.core.sales.api.model.money.TicketMoneyBreakdown;
import com.tchalanet.server.core.sales.api.model.origin.OfflineSaleRef;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketCodeGeneratorPort;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketCodes;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketContext;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketIdentity;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Maps a self-contained {@link OfflineSubmissionTicketDraft} into a {@link Ticket} aggregate.
 *
 * <p>The {@code drawId} comes from the draft itself — the cashier had it pinned at sale
 * time. We look up the matching {@link DrawSummary}
 * via {@link GetDrawByIdQuery} to populate {@link TicketContext#drawChannelId()}.
 *
 * <p>Compromises kept explicit for v1:
 * <ul>
 *   <li>{@code oddsSnapshot = 1} — the device-computed {@code potentialPayout} carries the
 *       canonical value already; sales does not re-price offline lines.</li>
 *   <li>{@code TicketMoneyBreakdown}: {@code stake == total} (no offline-specific fees).</li>
 *   <li>{@code Selection.displayLabel} reuses the raw {@code selectionKey} from the device.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class OfflineSubmissionToTicketMapper {

    private final QueryBus queryBus;
    private final TicketCodeGeneratorPort codeGenerator;
    private final IdGenerator idGenerator;

    public Ticket toTicket(
        TenantId tenantId,
        OfflineSubmissionTicketDraft draft,
        OfflineSaleRef offlineRef,
        Instant now
    ) {
        if (draft.lines().isEmpty()) {
            throw new IllegalArgumentException("draft has no lines");
        }

        // The draw was pinned by the device when the cashier sold offline — look it up
        // directly. Throws if the drawId no longer exists (sales rejects the promotionDecision).
        var draw = queryBus.ask(new GetDrawByIdQuery(draft.drawId()));

        var identity = new TicketIdentity(TicketId.of(idGenerator.newUuid()), tenantId);
        var context = new TicketContext(
            draft.outletId(), draft.terminalId(), draft.sellerUserId(),
            draft.salesSessionId(),
            draw.drawId(), draw.drawChannelId()
        );
        var codes = TicketCodes.of(
            codeGenerator.nextTicketCode(),
            codeGenerator.nextPublicCode(),
            codeGenerator.nextVerificationCode()
        );
        var breakdown = new TicketMoneyBreakdown(
            draft.totalStakeAmount(), List.of(), draft.totalStakeAmount());
        List<TicketLine> ticketLines = draft.lines().stream()
            .map(this::toTicketLine)
            .toList();

        return Ticket.place(
            identity, context, codes,
            breakdown, ticketLines,
            TicketSaleChannel.POS_OFFLINE_SYNCED,
            offlineRef,
            /* requiresApproval */ false,
            /* approvalRequestId */ null,
            draft.sellerUserId(),
            now
        );
    }

    private TicketLine toTicketLine(OfflineSubmissionLineSnapshot l) {
        Money stake = l.stakeAmount();
        Money potentialPayout = l.potentialPayout() != null ? l.potentialPayout() : stake;
        Short betOption = parseBetOption(l.betOption());
        return new TicketLine(
            TicketLineId.of(idGenerator.newUuid()),
            Math.max(1, l.lineNo()),
            GameCode.valueOf(l.gameCode()),
            BetType.valueOf(l.betType()),
            new Selection(SelectionKey.of(l.selectionKey()), l.selectionKey()),
            stake,
            BigDecimal.ONE,
            potentialPayout,
            betOption,
            TicketLineResultStatus.PENDING,
            Money.zero(stake.currency())
        );
    }

    private static Short parseBetOption(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Short.parseShort(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
