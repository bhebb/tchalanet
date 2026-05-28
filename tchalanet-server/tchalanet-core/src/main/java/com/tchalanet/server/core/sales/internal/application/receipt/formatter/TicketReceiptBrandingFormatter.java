package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.core.sales.api.model.receipt.ReceiptBrandingDisplayMode;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TicketReceiptBrandingFormatter {

    public List<TicketReceiptTextLine> headerLines(TicketReceiptView receipt) {
        return headerLines(receipt, ReceiptBrandingDisplayMode.AUTO, ReceiptBrandingDisplayMode.AUTO);
    }

    public List<TicketReceiptTextLine> headerLines(
        TicketReceiptView receipt,
        ReceiptBrandingDisplayMode tenantMode,
        ReceiptBrandingDisplayMode outletMode
    ) {
        var lines = new ArrayList<TicketReceiptTextLine>();
        addBranding(lines, firstNonBlank(receipt.tenantDisplayName(), "TCHALANET"), receipt.tenantReceiptHeader(), tenantMode);
        addBranding(lines, receipt.outletName(), receipt.outletReceiptHeader(), outletMode);
        return List.copyOf(lines);
    }

    public List<TicketReceiptTextLine> footerLines(TicketReceiptView receipt) {
        var lines = new ArrayList<TicketReceiptTextLine>();
        add(lines, receipt.outletReceiptFooter(), false);
        add(lines, receipt.tenantReceiptFooter(), false);
        return List.copyOf(lines);
    }

    private void add(List<TicketReceiptTextLine> lines, String value, boolean bold) {
        if (value != null && !value.isBlank()) {
            lines.add(bold ? TicketReceiptTextLine.bold(value) : TicketReceiptTextLine.normal(value));
        }
    }

    private void addBranding(
        List<TicketReceiptTextLine> lines,
        String name,
        String header,
        ReceiptBrandingDisplayMode mode
    ) {
        switch (mode == null ? ReceiptBrandingDisplayMode.AUTO : mode) {
            case NAME_ONLY -> add(lines, name, true);
            case HEADER_ONLY -> add(lines, header, false);
            case NAME_AND_HEADER -> {
                add(lines, name, true);
                add(lines, header, false);
            }
            case AUTO -> {
                if (header != null && !header.isBlank()) {
                    add(lines, header, false);
                } else {
                    add(lines, name, true);
                }
            }
        }
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
