package com.tchalanet.server.core.sales.internal.application.receipt;

import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLine;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptGameSectionView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketPublicCodeFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketVerificationUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TicketReceiptAssembler {

    private final TicketPublicCodeFormatter publicCodeFormatter;
    private final TicketVerificationUrlBuilder verificationUrlBuilder;

    public TicketReceiptView assemble(TicketPrintView printView, Locale requestedLocale) {
        Objects.requireNonNull(printView, "ticket_receipt.print_view_required");
        Objects.requireNonNull(printView.identity(), "ticket_receipt.identity_required");
        Objects.requireNonNull(printView.lifecycle(), "ticket_receipt.lifecycle_required");
        Objects.requireNonNull(printView.draw(), "ticket_receipt.draw_required");
        Objects.requireNonNull(printView.context(), "ticket_receipt.context_required");
        Objects.requireNonNull(printView.money(), "ticket_receipt.money_required");
        Objects.requireNonNull(printView.metadata(), "ticket_receipt.metadata_required");
        var locale = requestedLocale != null
            ? requestedLocale
            : printView.metadata().locale();
        if (locale == null) {
            locale = Locale.FRENCH;
        }
        var publicCode = publicCodeFormatter.normalize(printView.identity().publicCode());
        var displayCode = publicCodeFormatter.display(publicCode);
        var verificationUrl = verificationUrlBuilder.buildUrl(publicCode);

        return new TicketReceiptView(
            printView.identity().ticketId(),
            printView.identity().tenantId(),
            printView.identity().ticketCode(),
            displayCode,
            publicCode,
            printView.identity().verificationCode(),
            printView.lifecycle().saleStatus(),
            printView.lifecycle().resultStatus(),
            printView.lifecycle().settlementStatus(),
            printView.branding() == null ? null : printView.branding().tenantDisplayName(),
            printView.branding() == null ? null : printView.branding().tenantReceiptHeader(),
            printView.branding() == null ? null : printView.branding().outletReceiptHeader(),
            // drawLabel: keep slot alias if present; drawChannelLabel: preferred market/channel name
            printView.draw().label(),
            printView.draw().drawChannelName(),
            printView.draw().scheduledAt(),
            printView.context().outletName(),
            printView.context().terminalCode(),
            printView.context().sellerDisplayName(),
            printView.metadata().placedAt(),
            locale,
            printView.metadata().timezone(),
            gameSections(printView.lines()),
            printView.money().stake(),
            printView.money().totalAmount(),
            printView.money().potentialPayoutAmount(),
            printView.branding() == null ? null : printView.branding().outletReceiptFooter(),
            printView.branding() == null ? null : printView.branding().tenantReceiptFooter(),
            verificationUrl
        );
    }

    private List<TicketReceiptGameSectionView> gameSections(List<TicketPrintLine> lines) {
        var grouped = new LinkedHashMap<String, List<TicketPrintLine>>();
        for (var line : lines) {
            grouped.computeIfAbsent(line.gameCode().name(), ignored -> new java.util.ArrayList<>()).add(line);
        }
        return grouped.entrySet().stream()
            .map(entry -> new TicketReceiptGameSectionView(
                entry.getKey(),
                entry.getValue().getFirst().gameLabel(),
                entry.getValue().stream().map(this::lineView).toList()))
            .toList();
    }

    private TicketReceiptLineView lineView(TicketPrintLine line) {
        return new TicketReceiptLineView(
            line.lineNo(),
            line.gameCode().name(),
            line.betType().name(),
            line.betOption(),
            optionLabel(line),
            line.gameLabel(),
            line.selectionCanonical(),
            line.odds(),
            line.stake(),
            line.potentialPayout(),
            line.promotional(),
            line.promotionLabel(),
            line.promotionEffectType()
        );
    }

    private String optionLabel(TicketPrintLine line) {
        try {
            var option = BetOption.from(line.betType(), line.betOption());
            return option == null ? null : option.label();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
