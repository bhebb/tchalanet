package com.tchalanet.server.core.sales.internal.application.receipt;

import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptQrView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptSectionContent;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptPrintContent;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptBrandingFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptFactsFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptGameLinesFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketReceiptPrintFormatter {

    private final TicketReceiptBrandingFormatter brandingFormatter;
    private final TicketReceiptFactsFormatter factsFormatter;
    private final TicketReceiptGameLinesFormatter gameLinesFormatter;
    private final TicketReceiptI18nResolver i18nResolver;

    public TicketReceiptPrintContent format(TicketReceiptView receipt, PrintOutputFormat format) {
        var translations = i18nResolver.resolve(receipt.locale(), receipt.tenantId());
        var header = new ArrayList<TicketReceiptTextLine>();
        header.addAll(brandingFormatter.headerLines(receipt));
        add(header, "--------------------------------");
        header.addAll(factsFormatter.ticketIdentityLines(receipt, translations));

        var sections = new ArrayList<TicketReceiptSectionContent>();
        sections.add(factsFormatter.drawSection(receipt, translations));

        for (var section : receipt.gameSections()) {
            sections.add(new TicketReceiptSectionContent(
                section.gameLabel() == null ? section.gameCode() : section.gameLabel(),
                gameLinesFormatter.format(section.lines(), translations)
            ));
        }

        var totals = new ArrayList<TicketReceiptTextLine>();
        addLabel(totals, translations.text(TicketReceiptI18nKeys.TOTAL_STAKE), receipt.stakeTotal(), false);
        addLabel(totals, translations.text(TicketReceiptI18nKeys.TOTAL_AMOUNT), receipt.totalAmount(), true);
        addLabel(totals, translations.text(TicketReceiptI18nKeys.TOTAL_MAX_PAYOUT), receipt.potentialPayout(), true);

        var footer = new ArrayList<TicketReceiptTextLine>();
        add(footer, "--------------------------------");
        footer.addAll(brandingFormatter.footerLines(receipt));
        addLabel(footer, translations.text(TicketReceiptI18nKeys.VERIFICATION), receipt.verificationUrl(), false);
        addLabel(footer, translations.text(TicketReceiptI18nKeys.QR), receipt.displayCode(), false);

        return new TicketReceiptPrintContent(
            firstNonBlank(receipt.tenantDisplayName(), "Ticket Tchalanet"),
            header,
            sections,
            totals,
            footer,
            new TicketReceiptQrView(receipt.verificationUrl(), receipt.verificationUrl()),
            "ticket-" + receipt.displayCode(),
            receipt.locale(),
            receipt.timezone(),
            Map.of(
                "ticketId", receipt.ticketId().value().toString(),
                "publicCode", receipt.publicCode(),
                "displayCode", receipt.displayCode(),
                "format", format.name()
            )
        );
    }

    private void add(List<TicketReceiptTextLine> lines, String value) {
        add(lines, value, false);
    }

    private void add(List<TicketReceiptTextLine> lines, String value, boolean bold) {
        if (value != null && !value.isBlank()) {
            lines.add(bold ? TicketReceiptTextLine.bold(value) : TicketReceiptTextLine.normal(value));
        }
    }

    private void addLabel(List<TicketReceiptTextLine> lines, String label, String value, boolean bold) {
        if (value != null && !value.isBlank()) {
            add(lines, label + ": " + value, bold);
        }
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
