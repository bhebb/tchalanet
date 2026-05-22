package com.tchalanet.server.features.cashier.tickets.mapper;

import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLine;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.features.cashier.tickets.model.PrintTicketRequest;
import com.tchalanet.server.platform.document.api.model.DocumentAsset;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentKind;
import com.tchalanet.server.platform.document.api.model.DocumentLine;
import com.tchalanet.server.platform.document.api.model.DocumentOptions;
import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.DocumentSection;
import com.tchalanet.server.platform.document.api.model.DocumentTemplateKey;
import com.tchalanet.server.platform.document.api.model.LineStyle;
import com.tchalanet.server.platform.document.api.model.PaperSize;
import com.tchalanet.server.platform.document.api.model.ReceiptDocumentContent;
import com.tchalanet.server.platform.tenantconfig.api.model.view.TenantInternalDocumentConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TicketPrintDocumentMapper {

    private static final String SYSTEM_FOOTER_FALLBACK = "Merci et bonne chance";

    public DocumentRenderRequest toRenderRequest(
        TicketPrintView view,
        PrintTicketRequest request,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig
    ) {
        var content = new ReceiptDocumentContent(
            headerLines(view, tenantReceiptConfig),
            sections(view),
            totals(view, tenantReceiptConfig),
            footerLines(view, tenantReceiptConfig)
        );

        return new DocumentRenderRequest(
            templateKey(request.format(), tenantReceiptConfig),
            DocumentKind.RECEIPT,
            toDocumentFormat(request.format()),
            documentTitle(view, tenantReceiptConfig),
            content,
            assets(view, request, tenantReceiptConfig),
            options(tenantReceiptConfig),
            view.metadata().locale(),
            view.metadata().timezone(),
            metadata(view)
        );
    }

    private String documentTitle(
        TicketPrintView view,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig
    ) {
        var displayName = resolveDisplayName(view, tenantReceiptConfig);
        return displayName == null || displayName.isBlank()
            ? "TCHALANET"
            : displayName;
    }

    private List<DocumentLine> headerLines(
        TicketPrintView view,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig
    ) {
        var lines = new ArrayList<DocumentLine>();

        addIfPresent(lines, resolveHeaderMessage(view, tenantReceiptConfig), LineStyle.NORMAL);

        if (showOutletName(tenantReceiptConfig)) {
            addIfPresent(lines, view.context().outletName(), LineStyle.NORMAL);
        }

        addIfPresent(lines, resolveOutletHeader(view), LineStyle.NORMAL);
        lines.add(line("--------------------------------", LineStyle.NORMAL));

        lines.add(line("Ticket: " + view.identity().ticketCode(), LineStyle.BOLD));
        lines.add(line("Code:   " + dashedPublicCode(view.identity().publicCode()), LineStyle.BOLD));
        lines.add(line("Vente:  " + formatPlacedAt(view), LineStyle.SMALL));
        lines.add(line("Terminal: " + view.context().terminalCode(), LineStyle.NORMAL));

        if (showSellerName(tenantReceiptConfig)) {
            lines.add(line("Vendeur: " + view.context().sellerDisplayName(), LineStyle.NORMAL));
        }

        lines.add(line("--------------------------------", LineStyle.NORMAL));

        return lines;
    }

    private List<DocumentLine> footerLines(
        TicketPrintView view,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig
    ) {
        var lines = new ArrayList<DocumentLine>();

        // Top separator — guarantees the message block doesn't glue to the totals block.
        lines.add(line("--------------------------------", LineStyle.NORMAL));

        int messageStart = lines.size();
        addIfPresent(lines, resolveOutletFooter(view), LineStyle.NORMAL);
        addIfPresent(lines, resolveFooterMessage(view, tenantReceiptConfig), LineStyle.NORMAL);
        if (lines.size() == messageStart) {
            lines.add(line(SYSTEM_FOOTER_FALLBACK, LineStyle.NORMAL));
        }

        if (showQrCode(tenantReceiptConfig)) {
            // Separator between the goodbye message and the verification block.
            lines.add(line("--------------------------------", LineStyle.NORMAL));
            lines.add(line("Verif:", LineStyle.SMALL));
            lines.add(line(displayVerificationUrl(view), LineStyle.SMALL));
            // Empty line above the QR asset so it doesn't glue to the URL text.
            lines.add(line("", LineStyle.NORMAL));
        }

        if (view.metadata().disclaimers() != null) {
            view.metadata().disclaimers().values()
                .forEach(disclaimer -> lines.add(line(disclaimer, LineStyle.SMALL)));
        }

        return lines;
    }

    private DocumentTemplateKey templateKey(
        PrintOutputFormat format,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig
    ) {
        if (receiptConfigEnabled(tenantReceiptConfig)
            && tenantReceiptConfig.defaultTemplateKey() != null
            && !tenantReceiptConfig.defaultTemplateKey().isBlank()) {
            return DocumentTemplateKey.of(tenantReceiptConfig.defaultTemplateKey());
        }

        return switch (format) {
            case PDF -> DocumentTemplateKey.of("sales.ticket.receipt.pdf.v1");
            case ESC_POS -> DocumentTemplateKey.of("sales.ticket.receipt.escpos.v1");
        };
    }

    private DocumentOptions options(TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig) {
        var paperSize = parsePaperSize(tenantReceiptConfig);
        return switch (paperSize) {
            case RECEIPT_58MM -> DocumentOptions.receipt58mm();
            case A4, RECEIPT_80MM -> DocumentOptions.receipt80mm();
        };
    }

    private PaperSize parsePaperSize(TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig) {
        if (!receiptConfigEnabled(tenantReceiptConfig)
            || tenantReceiptConfig.defaultPaperSize() == null
            || tenantReceiptConfig.defaultPaperSize().isBlank()) {
            return PaperSize.RECEIPT_80MM;
        }

        try {
            return PaperSize.valueOf(tenantReceiptConfig.defaultPaperSize().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PaperSize.RECEIPT_80MM;
        }
    }

    private List<DocumentAsset> assets(
        TicketPrintView view,
        PrintTicketRequest request,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig
    ) {
        if (!showQrCode(tenantReceiptConfig)) {
            return List.of();
        }
        return List.of(DocumentAsset.qr(view.qr().payload(), qrSize(request.format())));
    }

    private DocumentFormat toDocumentFormat(PrintOutputFormat format) {
        return switch (format) {
            case PDF -> DocumentFormat.PDF;
            case ESC_POS -> DocumentFormat.ESC_POS;
        };
    }

    private int qrSize(PrintOutputFormat format) {
        return switch (format) {
            case PDF -> 300;
            case ESC_POS -> 280;
        };
    }

    private List<DocumentSection> sections(TicketPrintView view) {
        return List.of(
            new DocumentSection("Tirage", drawLines(view)),
            new DocumentSection("Jeux", gameSectionLines(view))
        );
    }

    private List<DocumentLine> drawLines(TicketPrintView view) {
        var lines = new ArrayList<DocumentLine>();
        var draw = view.draw();
        if (draw == null) {
            return lines;
        }
        var channelLabel = draw.label() == null ? draw.drawChannelName() : draw.label();
        // The label produced upstream sometimes embeds " - {scheduledAt}". Split on the first
        // " - " to keep the channel description on its own line above the scheduled time.
        if (channelLabel != null) {
            int sep = channelLabel.indexOf(" - ");
            if (sep > 0) {
                lines.add(line(channelLabel.substring(0, sep), LineStyle.BOLD));
            } else {
                lines.add(line(channelLabel, LineStyle.BOLD));
            }
        }
        if (draw.scheduledAt() != null) {
            var zone = view.metadata() != null && view.metadata().timezone() != null
                ? view.metadata().timezone()
                : java.time.ZoneId.systemDefault();
            lines.add(line(DRAW_SCHEDULED_FMT.withZone(zone).format(draw.scheduledAt()), LineStyle.NORMAL));
        }
        return lines;
    }

    // Column widths for the per-game tabular layout (fits within 32 chars / 80mm thermal).
    private static final int COL_SELECTION = 8;
    private static final int COL_STAKE = 8;
    private static final int COL_GAIN = 12;

    private List<DocumentLine> gameSectionLines(TicketPrintView view) {
        var lines = new ArrayList<DocumentLine>();
        // Group successive lines by (gameCode, betType, betOption). Preserves the cart order.
        // TODO (cashier-sales-v1, future): optional compact mode that collapses duplicate
        // (selection, stake) rows into a single row showing the cumulative stake + payout.
        // For now we keep one row per cart line so the client can see exactly what was sold.
        String currentKey = null;
        for (var line : view.lines()) {
            var key = line.gameCode().name()
                + "|" + line.betType().name()
                + "|" + (line.betOption() == null ? "" : line.betOption());
            if (!key.equals(currentKey)) {
                // No blank line between groups — the bold section header is enough visual
                // separation, and a blank line balloons the receipt height on long tickets.
                lines.add(line(gameSectionHeader(line), LineStyle.BOLD));
                lines.add(line(columnHeader(), LineStyle.SMALL));
                currentKey = key;
            }
            lines.add(line(tableRow(line), LineStyle.NORMAL));
        }
        return lines;
    }

    private static String columnHeader() {
        return rightPad("No", COL_SELECTION)
            + leftPad("Mise", COL_STAKE)
            + leftPad("Gain", COL_GAIN);
    }

    private static String tableRow(TicketPrintLine line) {
        return rightPad(line.selectionCanonical(), COL_SELECTION)
            + leftPad(formatAmount(line.stake()), COL_STAKE)
            + leftPad(formatAmount(line.potentialPayout()), COL_GAIN);
    }

    private String gameSectionHeader(TicketPrintLine line) {
        var game = humanGameLabel(line.gameCode());
        var option = humanBetOptionLabel(line.betType(), line.betOption());
        return option == null || option.isBlank() ? game : game + " - " + option;
    }

    private static String humanGameLabel(com.tchalanet.server.catalog.game.api.model.GameCode code) {
        if (code == null) return "";
        // HT_LOTO3 → "LOTO 3"; HT_BOLET → "BOLET"; HT_MARYAJ → "MARYAJ".
        var raw = code.name();
        if (raw.startsWith("HT_")) raw = raw.substring(3);
        return switch (raw) {
            case "LOTO3" -> "LOTO 3";
            case "LOTO4" -> "LOTO 4";
            case "LOTO5" -> "LOTO 5";
            default -> raw;
        };
    }

    private static String humanBetOptionLabel(
        com.tchalanet.server.catalog.game.api.model.BetType betType,
        Short betOption
    ) {
        if (betType == null || betOption == null) {
            return null;
        }
        try {
            var option = com.tchalanet.server.catalog.game.api.model.BetOption.from(betType, betOption);
            return option == null ? null : option.label();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String displayVerificationUrl(TicketPrintView view) {
        var url = view.qr() == null ? null : view.qr().verificationUrl();
        if (url == null || url.isBlank()) return "";
        // Strip protocol for human readability on the receipt (QR keeps the full URL).
        var shown = url.startsWith("https://") ? url.substring(8)
                  : url.startsWith("http://") ? url.substring(7)
                  : url;
        // Inject the dash into the trailing code segment if it looks like /ticket/XXXXXXXX.
        int slash = shown.lastIndexOf('/');
        if (slash > 0 && slash < shown.length() - 1) {
            var tail = shown.substring(slash + 1);
            if (tail.matches("[A-Z0-9]{8}")) {
                shown = shown.substring(0, slash + 1) + tail.substring(0, 4) + "-" + tail.substring(4);
            }
        }
        return shown;
    }

    private static String dashedPublicCode(String publicCode) {
        if (publicCode == null || publicCode.isBlank()) return "";
        var clean = publicCode.replace("-", "").replace(" ", "");
        if (clean.length() != 8) return publicCode;
        return clean.substring(0, 4) + "-" + clean.substring(4);
    }

    private static String rightPad(String value, int width) {
        var s = value == null ? "" : value;
        if (s.length() >= width) return s + " ";
        return s + " ".repeat(width - s.length());
    }

    private static String leftPad(String value, int width) {
        var s = value == null ? "" : value;
        if (s.length() >= width) return s;
        return " ".repeat(width - s.length()) + s;
    }

    /** Amount only (no currency suffix), used in the per-line tabular layout. */
    private static String formatAmount(com.tchalanet.server.common.types.money.Money m) {
        if (m == null) {
            return "";
        }
        var amount = m.amount().setScale(2, java.math.RoundingMode.HALF_UP);
        return MONEY_FORMAT.format(amount);
    }

    // Width of the totals block: label left-padded, amount+currency right-aligned.
    private static final int TOTALS_WIDTH = 32;

    private List<DocumentLine> totals(
        TicketPrintView view,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig
    ) {
        var lines = new ArrayList<DocumentLine>();

        // Visual break between the games block and the totals block:
        // empty line + separator → guarantees the previous "Gain" amount doesn't glue
        // to the "Mise:" total under it.
        lines.add(line("", LineStyle.NORMAL));
        lines.add(line("--------------------------------", LineStyle.NORMAL));
        lines.add(totalLine("Mise:", formatMoney(view.money().stake()), LineStyle.NORMAL));

        for (var charge : view.money().charges()) {
            if (charge.paidBy().name().equals("BUYER")) {
                lines.add(totalLine(charge.label() + ":", formatMoney(charge.amount()), LineStyle.NORMAL));
            }
        }

        lines.add(totalLine("TOTAL:", formatMoney(view.money().totalAmount()), LineStyle.BOLD));
        if (showPotentialPayout(tenantReceiptConfig)) {
            lines.add(totalLine("Gain max:", formatMoney(view.money().potentialPayoutAmount()), LineStyle.BOLD));
        }

        return lines;
    }

    private DocumentLine totalLine(String label, String amountWithCurrency, LineStyle style) {
        var l = label == null ? "" : label;
        var r = amountWithCurrency == null ? "" : amountWithCurrency;
        int spacing = Math.max(1, TOTALS_WIDTH - l.length() - r.length());
        return line(l + " ".repeat(spacing) + r, style);
    }

    private DocumentLine line(String text, LineStyle style) {
        return new DocumentLine(text, style);
    }

    // ─── Formatters ─────────────────────────────────────────────────────────

    private static final java.text.DecimalFormat MONEY_FORMAT = buildMoneyFormat();
    private static final java.time.format.DateTimeFormatter PLACED_AT_FMT =
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // Draw scheduledAt is shown to the minute only — seconds add noise for a draw time.
    private static final java.time.format.DateTimeFormatter DRAW_SCHEDULED_FMT =
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static java.text.DecimalFormat buildMoneyFormat() {
        var symbols = new java.text.DecimalFormatSymbols(java.util.Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator('.');
        var df = new java.text.DecimalFormat("#,##0.00", symbols);
        df.setGroupingSize(3);
        return df;
    }

    private static String formatMoney(com.tchalanet.server.common.types.money.Money m) {
        if (m == null) {
            return "";
        }
        var amount = m.amount().setScale(2, java.math.RoundingMode.HALF_UP);
        return MONEY_FORMAT.format(amount) + " " + m.currency().value();
    }

    private static String formatPlacedAt(TicketPrintView view) {
        var placed = view.metadata() == null ? null : view.metadata().placedAt();
        if (placed == null) {
            return "";
        }
        var zone = view.metadata().timezone();
        var z = zone == null ? java.time.ZoneId.systemDefault() : zone;
        return PLACED_AT_FMT.withZone(z).format(placed);
    }

    private boolean addIfPresent(List<DocumentLine> lines, String value, LineStyle style) {
        if (value == null || value.isBlank()) {
            return false;
        }
        lines.add(line(value, style));
        return true;
    }

    private String resolveDisplayName(
        TicketPrintView view,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig
    ) {
        if (receiptConfigEnabled(tenantReceiptConfig)
            && tenantReceiptConfig.displayName() != null
            && !tenantReceiptConfig.displayName().isBlank()) {
            return tenantReceiptConfig.displayName();
        }
        return view.branding() == null ? null : view.branding().tenantDisplayName();
    }

    private String resolveHeaderMessage(
        TicketPrintView view,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig
    ) {
        if (receiptConfigEnabled(tenantReceiptConfig)
            && tenantReceiptConfig.headerMessage() != null
            && !tenantReceiptConfig.headerMessage().isBlank()) {
            return tenantReceiptConfig.headerMessage();
        }
        return view.branding() == null ? null : view.branding().tenantReceiptHeader();
    }

    private String resolveFooterMessage(
        TicketPrintView view,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig
    ) {
        if (receiptConfigEnabled(tenantReceiptConfig)
            && tenantReceiptConfig.footerMessage() != null
            && !tenantReceiptConfig.footerMessage().isBlank()) {
            return tenantReceiptConfig.footerMessage();
        }
        return view.branding() == null ? null : view.branding().tenantReceiptFooter();
    }

    private String resolveOutletHeader(TicketPrintView view) {
        return view.branding() == null ? null : view.branding().outletReceiptHeader();
    }

    private String resolveOutletFooter(TicketPrintView view) {
        return view.branding() == null ? null : view.branding().outletReceiptFooter();
    }

    private boolean showQrCode(TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig) {
        return !receiptConfigEnabled(tenantReceiptConfig) || tenantReceiptConfig.showQrCode();
    }

    private boolean showSellerName(TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig) {
        return !receiptConfigEnabled(tenantReceiptConfig) || tenantReceiptConfig.showSellerName();
    }

    private boolean showOutletName(TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig) {
        return !receiptConfigEnabled(tenantReceiptConfig) || tenantReceiptConfig.showOutletName();
    }

    private boolean showPotentialPayout(TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig) {
        return !receiptConfigEnabled(tenantReceiptConfig) || tenantReceiptConfig.showPotentialPayout();
    }

    private boolean receiptConfigEnabled(TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig) {
        return tenantReceiptConfig != null && tenantReceiptConfig.enabled();
    }

    private Map<String, String> metadata(TicketPrintView view) {
        return Map.of(
            "filename", "ticket-" + view.identity().ticketCode(),
            "ticketId", view.identity().ticketId().value().toString(),
            "publicCode", view.identity().publicCode(),
            "verificationCode", view.identity().verificationCode()
        );
    }
}
