package com.tchalanet.server.features.publicdraw;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import com.tchalanet.server.features.publicdraw.model.*;
import com.tchalanet.server.features.publicdraw.model.PublicLatestDrawResultsResponse;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PublicDrawResultMapper {

    public PublicDrawResultListResponse toListResponse(TchPage<DrawSummary> page) {
        return new PublicDrawResultListResponse(
            page.items().stream().map(this::toItem).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages());
    }


    public PublicDrawResultDetailsResponse toDetails(DrawSummary s) {
        var result = s.result();
        var haiti = result == null || result.haitiResult() == null
            ? Map.<String, Object>of()
            : result.haitiResult();

        return new PublicDrawResultDetailsResponse(
            s.drawId().value().toString(),
            s.resultSlotKey(),
            s.resultProvider(),
            s.resultTimezone(),
            s.resultDrawTime() == null ? null : s.resultDrawTime().toString(),
            s.drawDate(),
            result == null ? null : result.occurredAt(),
            textOrNull(haiti, "lot1"),
            textOrNull(haiti, "lot2"),
            textOrNull(haiti, "lot3"),
            textOrNull(haiti, "lot4"),
            result == null || result.status() == null ? null : result.status().name(), null, null);
//            result == null ? null : result.quality(),
//            result == null ? null : result.source());
    }


    public PublicLatestDrawResultsPageResponse toLatestPageResponse(
        TchPage<DrawSummary> page,
        int limitPerSlot) {

        var groups = toLatestGroups(page.items(), limitPerSlot);

        return new PublicLatestDrawResultsPageResponse(
            groups,
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages());
    }

    public List<PublicLatestDrawResultsResponse> toLatestGroups(
        List<DrawSummary> summaries,
        int limitPerSlot) {

        if (summaries == null || summaries.isEmpty()) {
            return List.of();
        }

        Map<String, List<DrawSummary>> grouped =
            summaries.stream()
                .filter(s -> s.resultSlotKey() != null)
                .collect(
                    Collectors.groupingBy(
                        DrawSummary::resultSlotKey,
                        LinkedHashMap::new,
                        Collectors.toList()));

        return grouped.entrySet().stream()
            .map(
                e -> {
                    var first = e.getValue().getFirst();

                    var items =
                        e.getValue().stream()
                            .limit(limitPerSlot)
                            .map(this::toItem)
                            .toList();

                    return new PublicLatestDrawResultsResponse(
                        first.resultSlotKey(),
                        first.resultProvider(),
                        first.resultTimezone(),
                        first.resultDrawTime() == null ? null : first.resultDrawTime().toString(),
                        null,
                        items);
                })
            .toList();
    }

    public PublicDrawResultItemResponse toItem(DrawSummary s) {
        var result = s.result();
        var haiti =
            result == null || result.haitiResult() == null
                ? Map.<String, Object>of()
                : result.haitiResult();

        return new PublicDrawResultItemResponse(
            s.drawId().value().toString(),
            s.resultSlotKey(),
            s.resultProvider(),
            s.resultTimezone(),
            s.resultDrawTime() == null ? null : s.resultDrawTime().toString(),
            s.drawDate(),
            result == null ? null : result.occurredAt(),
            textOrNull(haiti, "lot1"),
            textOrNull(haiti, "lot2"),
            textOrNull(haiti, "lot3"),
            textOrNull(haiti, "lot4"),
            result == null || result.status() == null ? null : result.status().name(),
            null, null);
        //            result == null ? null : result.quality(),
//            result == null ? null : result.source());
    }

    private static String textOrNull(Map<String, Object> map, String key) {
        var value = map.get(key);

        if (value == null) {
            return null;
        }

        var text = value.toString();

        return text.isBlank() ? null : text;
    }
}
