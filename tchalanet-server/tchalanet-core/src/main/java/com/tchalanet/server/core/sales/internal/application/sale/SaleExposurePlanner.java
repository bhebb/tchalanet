package com.tchalanet.server.core.sales.internal.application.sale;

import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.selection.api.SelectionApi;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SaleExposurePlanner {

    private final SelectionApi selectionApi;

    public Map<String, List<SellTicketLineInput>> groupByExposureKey(List<SellTicketLineInput> lines) {
        var grouped = new LinkedHashMap<String, List<SellTicketLineInput>>();
        for (var line : lines == null ? List.<SellTicketLineInput>of() : lines) {
            grouped.computeIfAbsent(exposureKey(line), ignored -> new java.util.ArrayList<>()).add(line);
        }
        return Map.copyOf(grouped);
    }

    private String exposureKey(SellTicketLineInput line) {
        var canonicalSelection =
            selectionApi.canonicalize(line.betType(), line.betOption(), line.rawSelection()).key().value();
        return line.gameCode() + "|" + line.betType() + "|" + line.betOption() + "|" + canonicalSelection;
    }
}
