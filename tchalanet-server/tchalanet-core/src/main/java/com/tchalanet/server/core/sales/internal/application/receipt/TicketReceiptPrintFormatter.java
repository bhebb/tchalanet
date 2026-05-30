package com.tchalanet.server.core.sales.internal.application.receipt;

import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptPrintContent;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptQrView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptSectionContent;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.ReceiptTextLayout;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptBrandingFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptFactsFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptGameLinesFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptLabelResolver;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptLayoutProfile;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptDrawFormatter;
import com.tchalanet.server.platform.document.api.model.DocumentPrintProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TicketReceiptPrintFormatter {

    private final TicketReceiptBrandingFormatter brandingFormatter;
    private final TicketReceiptFactsFormatter factsFormatter;
    private final TicketReceiptDrawFormatter drawFormatter;
    private final TicketReceiptGameLinesFormatter gameLinesFormatter;
    private final TicketReceiptLabelResolver labelResolver;
    private final TicketReceiptI18nResolver i18nResolver;
    private final ReceiptTextLayout layout;
    private final com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptMoneyFormatter moneyFormatter;

    public TicketReceiptPrintContent format(TicketReceiptView receipt, DocumentPrintProfile documentProfile) {
        var translations = i18nResolver.resolve(receipt.locale(), receipt.tenantId());
        var layoutProfile = TicketReceiptLayoutProfile.from(documentProfile);

        var header = new ArrayList<>(brandingFormatter.headerLines(receipt, layoutProfile));
        // separator between branding and ticket facts
        add(header, layout.separator(layoutProfile));
        header.addAll(factsFormatter.ticketIdentityLines(receipt, translations, layoutProfile));

        var sections = new ArrayList<TicketReceiptSectionContent>();
        // draw section (no separator between draw and games)
        sections.add(drawFormatter.drawSection(receipt, translations, layoutProfile));

        // optional single currency note for receipt paper
        var currencyNote = moneyFormatter.currencyNote(receipt.stakeTotal(), layoutProfile, translations);
        if (currencyNote != null && !currencyNote.isBlank()) {
            sections.add(new TicketReceiptSectionContent(
                null,
                List.of(TicketReceiptTextLine.normal(currencyNote))
            ));
        }

        for (var section : receipt.gameSections()) {
            var gameTitle = labelResolver.gameTitle(section, translations);
            var safeTitle = layout.truncate(gameTitle, layoutProfile.charsPerLine());
            sections.add(new TicketReceiptSectionContent(
                safeTitle,
                gameLinesFormatter.format(section.lines(), translations, layoutProfile)
            ));
        }

        var totals = new ArrayList<TicketReceiptTextLine>();
        // separator before money summary
        add(totals, layout.separator(layoutProfile));
        addLabel(totals, translations.text(TicketReceiptI18nKeys.TOTAL_STAKE), receipt.stakeTotal(), false, layoutProfile);
        addLabel(totals, translations.text(TicketReceiptI18nKeys.TOTAL_AMOUNT), receipt.totalAmount(), true, layoutProfile);
        addLabel(totals, translations.text(TicketReceiptI18nKeys.TOTAL_MAX_PAYOUT), receipt.potentialPayout(), true, layoutProfile);

        var footer = new ArrayList<TicketReceiptTextLine>();
        // separator before footer
        add(footer, layout.separator(layoutProfile));
        footer.addAll(brandingFormatter.footerLines(receipt, layoutProfile));

        if (layoutProfile.printFullVerificationUrl()) {
            addLabel(footer, translations.text(TicketReceiptI18nKeys.VERIFICATION), receipt.verificationUrl(), false, layoutProfile);
        } else {
            addLabel(footer, "Code", receipt.displayCode(), false, layoutProfile);
        }
        addLabel(footer, translations.text(TicketReceiptI18nKeys.QR), receipt.displayCode(), false, layoutProfile);

        // Assert no produced line exceeds the layout width (guard anti-overflow)
        assertLines("header", header, layoutProfile);
        for (var section : sections) {
            if (section.title() != null && section.title().length() > layoutProfile.charsPerLine()) {
                throw new IllegalStateException(
                    "Receipt section title exceeds width "
                        + layoutProfile.charsPerLine() + ": " + section.title()
                );
            }
            assertLines("section", section.lines(), layoutProfile);
        }
        assertLines("totals", totals, layoutProfile);
        assertLines("footer", footer, layoutProfile);

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
                "format", documentProfile.outputFormat().name(),
                "paperSize", documentProfile.paperSize().name()
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

    private void addLabel(List<TicketReceiptTextLine> lines, String label, Money value, boolean bold, TicketReceiptLayoutProfile profile) {
        if (value != null) {
            var formatted = moneyFormatter.format(value, profile);
            if (formatted != null && !formatted.isBlank()) {
                var text = layout.labelValue(label, formatted, profile);
                lines.add(bold ? TicketReceiptTextLine.bold(text) : TicketReceiptTextLine.normal(text));
            }
        }
    }

    private void addLabel(List<TicketReceiptTextLine> lines, String label, String value, boolean bold, TicketReceiptLayoutProfile profile) {
        if (value != null && !value.isBlank()) {
            var text = layout.labelValue(label, value, profile);
            lines.add(bold ? TicketReceiptTextLine.bold(text) : TicketReceiptTextLine.normal(text));
        }
    }

    private void assertLines(String part, List<TicketReceiptTextLine> lines, TicketReceiptLayoutProfile profile) {
        if (lines == null) {
            return;
        }
        for (var line : lines) {
            if (line.text().length() > profile.charsPerLine()) {
                throw new IllegalStateException(
                    "Receipt " + part + " line exceeds width "
                        + profile.charsPerLine() + ": " + line.text()
                );
            }
        }
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
