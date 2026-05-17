package com.tchalanet.server.features.cashier.print.mapper;

import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLine;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.features.cashier.print.PrintTicketRequest;
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
        lines.add(line("Public: " + view.identity().publicCode(), LineStyle.NORMAL));
        lines.add(line("Code: " + view.identity().verificationCode(), LineStyle.NORMAL));
        lines.add(line("Vente: " + view.metadata().placedAt(), LineStyle.SMALL));
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

        int footerStart = lines.size();

        addIfPresent(lines, resolveOutletFooter(view), LineStyle.NORMAL);
        addIfPresent(lines, resolveFooterMessage(view, tenantReceiptConfig), LineStyle.NORMAL);

        if (lines.size() == footerStart) {
            lines.add(line(SYSTEM_FOOTER_FALLBACK, LineStyle.NORMAL));
        }

        lines.add(line("--------------------------------", LineStyle.NORMAL));

        if (showQrCode(tenantReceiptConfig)) {
            lines.add(line("Scannez le QR pour verifier", LineStyle.SMALL));
            lines.add(line(view.qr().verificationUrl(), LineStyle.SMALL));
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
            new DocumentSection(
                "Tirage",
                List.of(line(view.draw().label(), LineStyle.BOLD))
            ),
            new DocumentSection(
                "Jeux",
                view.lines().stream()
                    .flatMap(this::lineRows)
                    .toList()
            )
        );
    }

    private java.util.stream.Stream<DocumentLine> lineRows(TicketPrintLine line) {
        return java.util.stream.Stream.of(
            line("#" + line.lineNo() + " " + line.gameCode(), LineStyle.BOLD),
            line("Selection: " + line.selectionCanonical(), LineStyle.NORMAL),
            line("Mise: " + line.stake(), LineStyle.NORMAL),
            line("Gain potentiel: " + line.potentialPayout(), LineStyle.NORMAL),
            line("--------------------------------", LineStyle.NORMAL)
        );
    }

    private List<DocumentLine> totals(
        TicketPrintView view,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig
    ) {
        var lines = new ArrayList<DocumentLine>();

        lines.add(line("Mise: " + view.money().stake(), LineStyle.NORMAL));

        for (var charge : view.money().charges()) {
            if (charge.paidBy().name().equals("BUYER")) {
                lines.add(line(charge.label() + ": " + charge.amount(), LineStyle.NORMAL));
            }
        }

        lines.add(line("TOTAL: " + view.money().totalAmount(), LineStyle.BOLD));
        if (showPotentialPayout(tenantReceiptConfig)) {
            lines.add(line("Gain max: " + view.money().potentialPayoutAmount(), LineStyle.BOLD));
        }

        return lines;
    }

    private DocumentLine line(String text, LineStyle style) {
        return new DocumentLine(text, style);
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
