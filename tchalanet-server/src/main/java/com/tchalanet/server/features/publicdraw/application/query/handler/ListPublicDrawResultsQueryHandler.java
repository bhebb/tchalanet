package com.tchalanet.server.features.publicdraw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPages;
import com.tchalanet.server.core.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.features.publicdraw.application.port.out.PublicDrawResultPort;
import com.tchalanet.server.features.publicdraw.application.query.model.ListPublicDrawResultsQuery;
import com.tchalanet.server.features.publicdraw.application.service.NextDrawCalculator;
import com.tchalanet.server.features.publicdraw.infra.web.mapper.PublicDrawResultMapper;
import com.tchalanet.server.features.publicdraw.infra.web.model.PublicDrawResultListResponse;
import com.tchalanet.server.features.publicdraw.infra.web.model.PublicNextDrawItem;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPublicDrawResultsQueryHandler
    implements QueryHandler<ListPublicDrawResultsQuery, PublicDrawResultListResponse> {

  private final PublicDrawResultPort port;
  private final PublicDrawResultMapper mapper;
  private final ResultSlotCatalog slotReader;
  private final NextDrawCalculator nextDraws;

  @Override
  public PublicDrawResultListResponse handle(ListPublicDrawResultsQuery q) {
    var page = port.search(q.slotKey(), q.provider(), q.from(), q.to(), q.pageable());

    var pageDto = TchPages.map(page, mapper::toItem);

    // next draws : selon le filtre slotKey/provider si fourni, sinon tous active
    var slots =
        (q.slotKey() != null && !q.slotKey().isBlank())
            ? slotReader
                .findBySlotKey(q.slotKey())
                .map(java.util.List::of)
                .orElse(java.util.List.of())
            : slotReader.listActive();

    var next =
        slots.stream()
            .filter(
                s ->
                    q.provider() == null
                        || q.provider().isBlank()
                        || q.provider().equalsIgnoreCase(s.provider()))
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

    return new PublicDrawResultListResponse(pageDto, next);
  }
}
