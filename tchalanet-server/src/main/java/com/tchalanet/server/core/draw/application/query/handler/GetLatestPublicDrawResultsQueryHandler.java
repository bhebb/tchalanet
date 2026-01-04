package com.tchalanet.server.core.draw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.draw.application.query.model.GetLatestPublicDrawResultsQuery;
import com.tchalanet.server.core.draw.application.port.out.PublicDrawResultPort;
import com.tchalanet.server.core.draw.application.print.DrawChannelLabelResolver;
import com.tchalanet.server.core.draw.infra.persistence.PublicDrawResultRow;
import com.tchalanet.server.core.draw.infra.web.model.PublicDrawResultItemResponse;
import com.tchalanet.server.core.draw.infra.web.model.PublicLatestDrawResultsResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetLatestPublicDrawResultsQueryHandler
    implements QueryHandler<
    GetLatestPublicDrawResultsQuery, List<PublicLatestDrawResultsResponse>> {

    private final PublicDrawResultPort port;
    private final JsonUtils jsonUtils;
    private final DrawChannelLabelResolver channelLabelResolver;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public List<PublicLatestDrawResultsResponse> handle(GetLatestPublicDrawResultsQuery query) {
        List<PublicDrawResultRow> rows = port.latest(query.limitPerChannel());

        return rows.stream()
            .collect(Collectors.groupingBy(PublicDrawResultRow::getChannelCode))
            .values()
            .stream()
            .map(this::mapToLatestResponse)
            .toList();
    }

    private PublicLatestDrawResultsResponse mapToLatestResponse(List<PublicDrawResultRow> rows) {
        PublicDrawResultRow first = rows.get(0);
        List<PublicDrawResultItemResponse> results =
            rows.stream().map(this::mapToItemResponse).collect(Collectors.toList());

        String channelLabel = channelLabelResolver.resolve(
            first.getChannelName(), first.getChannelDrawTime(), java.util.Locale.getDefault());

        return new PublicLatestDrawResultsResponse(
            first.getChannelCode(),
            channelLabel,
            first.getChannelTimezone().toString(),
            first.getChannelDrawTime().format(TIME_FORMATTER),
            results);
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
        String channelLabel = channelLabelResolver.resolve(
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
