package com.tchalanet.server.core.draw.application.query.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.draw.application.port.out.PublicDrawResultPort;
import com.tchalanet.server.core.draw.application.print.DrawChannelLabelResolver;
import com.tchalanet.server.core.draw.application.query.model.ListPublicDrawResultsQuery;
import com.tchalanet.server.core.draw.infra.persistence.PublicDrawResultRow;
import com.tchalanet.server.core.draw.infra.web.model.PublicDrawResultItemResponse;
import com.tchalanet.server.core.draw.infra.web.model.PublicDrawResultPageResponse;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListPublicDrawResultsQueryHandler
    implements QueryHandler<ListPublicDrawResultsQuery, PublicDrawResultPageResponse> {

  private final PublicDrawResultPort port;
  private final JsonUtils jsonUtils;
  private final DrawChannelLabelResolver channelLabelResolver;

  @Override
  public PublicDrawResultPageResponse handle(ListPublicDrawResultsQuery query) {
    Page<PublicDrawResultRow> page =
        port.search(query.channelCode(), query.from(), query.to(), query.pageable());

    List<PublicDrawResultItemResponse> items =
        page.getContent().stream().map(this::mapToItemResponse).collect(Collectors.toList());

    return new PublicDrawResultPageResponse(
        items, page.getNumber(), items.size(), page.getTotalElements());
  }

  private List<Integer> mapNumbers(String jsonb) {
    if (jsonb == null || jsonb.isEmpty()) return Collections.emptyList();
    try {
      return jsonUtils.readValue(jsonb, new TypeReference<List<Integer>>() {});
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  private PublicDrawResultItemResponse mapToItemResponse(PublicDrawResultRow row) {
    String channelLabel =
        channelLabelResolver.resolve(
            row.getChannelName(), row.getChannelDrawTime(), java.util.Locale.getDefault());

    return new PublicDrawResultItemResponse(
        row.getChannelCode(),
        channelLabel,
        row.getDrawDate(),
        row.getOccurredAt(),
        mapNumbers(row.getNumbersMainJson()),
        mapNumbers(row.getNumbersExtraJson()),
        row.getQuality(),
        row.getSource());
  }
}
