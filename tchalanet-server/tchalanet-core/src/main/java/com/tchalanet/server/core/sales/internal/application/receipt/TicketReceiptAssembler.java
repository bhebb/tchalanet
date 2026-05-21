package com.tchalanet.server.core.sales.internal.application.receipt;

import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLine;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptGameSectionView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketPublicCodeFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptMoneyFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketVerificationUrlBuilder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

@Component
public class TicketReceiptAssembler {

    private final TicketReceiptMoneyFormatter moneyFormatter;
    private final TicketPublicCodeFormatter publicCodeFormatter;
    private final TicketVerificationUrlBuilder verificationUrlBuilder;

    public TicketReceiptAssembler(
        TicketReceiptMoneyFormatter moneyFormatter,
        TicketPublicCodeFormatter publicCodeFormatter,
        TicketVerificationUrlBuilder verificationUrlBuilder
    ) {
        this.moneyFormatter = moneyFormatter;
        this.publicCodeFormatter = publicCodeFormatter;
        this.verificationUrlBuilder = verificationUrlBuilder;
    }

    public TicketReceiptView assemble(TicketPrintView printView, Locale requestedLocale) {
        var locale = requestedLocale != null
            ? requestedLocale
            : printView.metadata() == null ? Locale.FRENCH : printView.metadata().locale();
        if (locale == null) {
            locale = Locale.FRENCH;
        }
        var publicCode = publicCodeFormatter.normalize(printView.identity().publicCode());
        var displayCode = publicCodeFormatter.display(publicCode);
        var verificationUrl = verificationUrlBuilder.buildUrl(publicCode);

        return new TicketReceiptView(
            printView.identity().ticketId(),
            printView.identity().ticketCode(),
            displayCode,
            publicCode,
            printView.identity().verificationCode(),
            printView.lifecycle().saleStatus(),
            printView.lifecycle().resultStatus(),
            printView.lifecycle().settlementStatus(),
            printView.draw().label(),
            printView.context().outletName(),
            printView.context().terminalCode(),
            printView.context().sellerDisplayName(),
            printView.metadata().placedAt(),
            locale,
            gameSections(printView.lines()),
            moneyFormatter.format(printView.money().stake()),
            moneyFormatter.format(printView.money().totalAmount()),
            moneyFormatter.format(printView.money().potentialPayoutAmount()),
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
            moneyFormatter.format(line.stake()),
            moneyFormatter.format(line.potentialPayout())
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
