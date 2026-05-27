package com.tchalanet.server.features.ticketverify;

import com.tchalanet.server.core.sales.api.model.verification.TicketVerificationView;
import com.tchalanet.server.features.ticketverify.model.TicketVerifyOutletView;
import com.tchalanet.server.features.ticketverify.model.TicketVerifyResponse;
import org.springframework.stereotype.Component;

@Component
public class TicketVerifyMapper {

    public TicketVerifyResponse toResponse(TicketVerificationView view) {
        return new TicketVerifyResponse(
            view.publicCode(),
            view.displayCode(),
            view.status(),
            view.totalAmount(),
            view.winningAmount(),
            view.placedAt(),
            view.outlet() != null ? new TicketVerifyOutletView(view.outlet().name()) : null,
            new TicketVerifyResponse.DrawView(
                view.draw().channelName(),
                view.draw().channelLabel(),
                view.draw().drawDate(),
                view.draw().scheduledAt()
            ),
            view.lines().stream()
                .map(l -> new TicketVerifyResponse.LineView(
                    l.lineNumber(),
                    l.gameDisplayName(),
                    l.betTypeLabel(),
                    l.optionLabel(),
                    l.selection(),
                    l.stake(),
                    l.potentialPayout(),
                    l.promotional(),
                    l.promotionLabel()
                ))
                .toList()
        );
    }
}
