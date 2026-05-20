package com.tchalanet.server.core.sales.internal.application.sale;

import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SaleExposurePlanner {

    public Map<String, List<SellTicketLineInput>> groupByExposureKey(List<SellTicketLineInput> lines) {
        var grouped = new LinkedHashMap<String, List<SellTicketLineInput>>();
        for (var line : lines == null ? List.<SellTicketLineInput>of() : lines) {
            grouped.computeIfAbsent(exposureKey(line), ignored -> new java.util.ArrayList<>()).add(line);
        }
        return Map.copyOf(grouped);
    }

    private String exposureKey(SellTicketLineInput line) {
        return line.gameCode() + "|" + line.betType() + "|" + normalize(line.rawSelection()) + "|" + line.betOption();
    }

    private String normalize(String selection) {
        return selection == null ? "" : selection.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
