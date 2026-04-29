package com.tchalanet.server.features.publicdraw.app;

import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.features.publicdraw.PublicDrawResultMapper;
import com.tchalanet.server.features.publicdraw.model.PublicDrawResultListResponse;
import com.tchalanet.server.features.publicdraw.model.PublicDrawResultSearchCriteria;
import com.tchalanet.server.features.publicdraw.model.PublicNextDrawItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicDrawResultSearchService {

  private final PublicDrawResultReader reader;
  private final PublicDrawResultMapper mapper;
  private final ResultSlotCatalog slotReader;
  private final NextDrawCalculator nextDraws;

  public PublicDrawResultListResponse search(PublicDrawResultSearchCriteria criteria) {
    var page =
        reader.search(
            criteria.slotKey(),
            criteria.provider(),
            criteria.from(),
            criteria.to(),
            criteria.pageable());

    var pageView = TchPageMapper.map(page, mapper::toItem);

    // next draws : selon le filtre slotKey/provider si fourni, sinon tous active
    var slots =
        (criteria.slotKey() != null && !criteria.slotKey().isBlank())
            ? slotReader
                .findByKey(criteria.slotKey())
                .map(s -> java.util.List.of(s))
                .orElse(java.util.List.of())
            : slotReader.listActive();

    var next =
        slots.stream()
            .filter(
                s ->
                    criteria.provider() == null
                        || criteria.provider().isBlank()
                        || criteria.provider().equalsIgnoreCase(s.provider()))
            .map(
                s ->
                    new PublicNextDrawItem(
                        s.slotKey(),
                        s.provider(),
                        s.timezone().getId(),
                        s.drawTime() == null ? null : s.drawTime().toString(),
                        nextDraws.nextScheduledAt(
                            s.timezone().getId(),
                            s.drawTime(),
                            null) // daysOfWeek not available on view
                        ))
            .toList();

    return new PublicDrawResultListResponse(pageView, next);
  }
}
