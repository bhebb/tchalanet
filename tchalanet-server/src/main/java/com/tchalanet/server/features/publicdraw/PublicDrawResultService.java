package com.tchalanet.server.features.publicdraw;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.GetDrawByIdQuery;
import com.tchalanet.server.core.draw.application.query.model.ListDrawsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListLatestDrawsWithResultsQuery;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import com.tchalanet.server.features.publicdraw.model.PublicDrawResultDetailsResponse;
import com.tchalanet.server.features.publicdraw.model.PublicDrawResultListResponse;
import com.tchalanet.server.features.publicdraw.model.PublicDrawResultSearchCriteria;
import com.tchalanet.server.features.publicdraw.model.PublicLatestDrawResultsPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicDrawResultService {

    private final QueryBus queryBus;
    private final PublicDrawResultMapper mapper;

    public PublicDrawResultListResponse search(PublicDrawResultSearchCriteria criteria) {
        var slotKeys =
            criteria.slotKey() == null || criteria.slotKey().isBlank()
                ? null
                : List.of(criteria.slotKey());

        var coreCriteria =
            DrawSearchCriteria.forResults(
                slotKeys,
                criteria.from(),
                criteria.to());

        TchPage<DrawSummary> page =
            queryBus.send(new ListDrawsQuery(coreCriteria, criteria.pageable()));

        return mapper.toListResponse(page);
    }

    public PublicDrawResultDetailsResponse getByDrawId(String drawIdRaw) {
        var drawId = DrawId.of(UUID.fromString(drawIdRaw));

        DrawSummary summary =
            queryBus.send(new GetDrawByIdQuery(drawId));

        return mapper.toDetails(summary);
    }


    public PublicLatestDrawResultsPageResponse latest(
        int limitPerSlot,
        List<String> slotKeys,
        Pageable pageable) {

        int limit = Math.max(1, Math.min(10, limitPerSlot));
        var normalizedSlotKeys = normalizeSlotKeys(slotKeys);

        TchPage<DrawSummary> page =
            queryBus.send(
                new ListLatestDrawsWithResultsQuery(normalizedSlotKeys, pageable));

        return mapper.toLatestPageResponse(page, limit);
    }

    private static List<String> normalizeSlotKeys(List<String> slotKeys) {
        if (slotKeys == null || slotKeys.isEmpty()) {
            return null;
        }

        var normalized =
            slotKeys.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toUpperCase())
                .distinct()
                .toList();

        return normalized.isEmpty() ? null : normalized;
    }

}
