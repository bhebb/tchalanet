package com.tchalanet.server.core.offlinesync.api.event;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.Money;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Self-contained ticket draft carried by offline promotionDecision events. Holds every field that
 * {@code core.sales} needs to materialise (or reject) a ticket — no callback into
 * {@code core.offlinesync} is allowed.
 *
 * <p>{@code drawId} is the draw the device was selling for at the time of the sale. It is
 * pinned by the cashier at the screen before producing the offline ticket, so the server
 * does not have to guess a draw from the timestamp.
 */
public record OfflineSubmissionTicketDraft(
    UserId sellerUserId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    UUID deviceId,
    DrawId drawId,
    Instant clientSoldAt,
    Money totalStakeAmount,
    int lineCount,
    String payloadHash,
    List<OfflineSubmissionLineSnapshot> lines
) {

    public OfflineSubmissionTicketDraft {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
