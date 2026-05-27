package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptTextLine;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TicketReceiptBrandingFormatter {

    public List<TicketReceiptTextLine> headerLines(TicketReceiptView receipt) {
        var lines = new ArrayList<TicketReceiptTextLine>();
        add(lines, firstNonBlank(receipt.tenantDisplayName(), "TCHALANET"), true);
        add(lines, receipt.tenantReceiptHeader(), false);
        add(lines, receipt.outletName(), true);
        add(lines, receipt.outletReceiptHeader(), false);
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

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
