package com.tchalanet.server.features.cashier.tickets.mapper;

import com.tchalanet.server.platform.document.api.model.PrintOptionsRequest;
import com.tchalanet.server.platform.document.api.model.DocumentPrintProfile;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptPrintContent;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineStyle;
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
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalDocumentConfig;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TicketPrintDocumentMapper {

    public DocumentRenderRequest toRenderRequest(
        TicketReceiptPrintContent receipt,
        PrintTicketRequest request,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig,
        DocumentPrintProfile profile
    ) {
        var content = new ReceiptDocumentContent(
            toDocumentLines(receipt.headerLines()),
            receipt.sections().stream()
                .map(section -> new DocumentSection(section.title(), toDocumentLines(section.lines())))
                .toList(),
            toDocumentLines(receipt.totals()),
            toDocumentLines(receipt.footerLines())
        );

        return new DocumentRenderRequest(
            templateKey(profile, tenantReceiptConfig),
            DocumentKind.RECEIPT,
            toDocumentFormat(profile),
            receipt.title(),
            content,
            assets(receipt, request, tenantReceiptConfig, profile),
            options(profile, tenantReceiptConfig),
            receipt.locale(),
            receipt.timezone(),
            receipt.metadata()
        );
    }

    private List<DocumentLine> toDocumentLines(List<TicketReceiptTextLine> lines) {
        return lines.stream()
            .map(line -> new DocumentLine(line.text(), toLineStyle(line.style())))
            .toList();
    }

    private LineStyle toLineStyle(TicketReceiptLineStyle style) {
        return switch (style) {
            case BOLD -> LineStyle.BOLD;
            case SMALL -> LineStyle.SMALL;
            case NORMAL -> LineStyle.NORMAL;
        };
    }

    private DocumentTemplateKey templateKey(
        DocumentPrintProfile profile,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig
    ) {
        if (receiptConfigEnabled(tenantReceiptConfig)
            && tenantReceiptConfig.defaultTemplateKey() != null
            && !tenantReceiptConfig.defaultTemplateKey().isBlank()) {
            return DocumentTemplateKey.of(tenantReceiptConfig.defaultTemplateKey());
        }

        var docFormat = profile == null || profile.outputFormat() == null
            ? DocumentFormat.PDF
            : profile.outputFormat();

        return switch (docFormat) {
            case PDF -> DocumentTemplateKey.of("sales.ticket.receipt.pdf.v1");
            case ESC_POS -> DocumentTemplateKey.of("sales.ticket.receipt.escpos.v1");
            case PNG -> null;
            case HTML_PREVIEW -> null;
        };
    }

    private DocumentFormat toDocumentFormat(DocumentPrintProfile profile) {
        if (profile == null || profile.outputFormat() == null) {
            return DocumentFormat.PDF;
        }
        return profile.outputFormat();
    }

    private List<DocumentAsset> assets(
        TicketReceiptPrintContent receipt,
        PrintTicketRequest request,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig,
        DocumentPrintProfile profile
    ) {
        if (!showQrCode(tenantReceiptConfig) || receipt.qr() == null || receipt.qr().payload() == null) {
            return List.of();
        }
        return List.of(DocumentAsset.qr(receipt.qr().payload(), qrSize(profile)));
    }

    private int qrSize(DocumentPrintProfile profile) {
        var docFormat = profile == null || profile.outputFormat() == null
            ? DocumentFormat.PDF
            : profile.outputFormat();
        return switch (docFormat) {
            case PDF -> 300;
            case ESC_POS -> 280;
            case PNG -> 0;
            case HTML_PREVIEW -> 0;
        };
    }

    private DocumentOptions options(
        DocumentPrintProfile profile,
        TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig
    ) {
        // Honor an explicit paper size from the print request first; fall back to
        // the tenant receipt config (which itself defaults to 80mm). The profile
        // resolver collapses "unspecified" into A4, so only an explicit RECEIPT_*
        // request overrides the tenant default.
        var requested = profile == null ? null : profile.paperSize();
        var paperSize = (requested == PaperSize.RECEIPT_58MM || requested == PaperSize.RECEIPT_80MM)
            ? requested
            : parsePaperSize(tenantReceiptConfig);
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

    private boolean showQrCode(TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig) {
        return !receiptConfigEnabled(tenantReceiptConfig) || tenantReceiptConfig.showQrCode();
    }

    private boolean receiptConfigEnabled(TenantInternalDocumentConfig.ReceiptConfig tenantReceiptConfig) {
        return tenantReceiptConfig != null && tenantReceiptConfig.enabled();
    }
}
