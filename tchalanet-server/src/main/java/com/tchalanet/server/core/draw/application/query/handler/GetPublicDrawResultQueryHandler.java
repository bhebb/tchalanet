package com.tchalanet.server.core.draw.application.query.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.draw.application.port.out.PublicDrawResultPort;
import com.tchalanet.server.core.draw.application.print.DrawChannelLabelResolver;
import com.tchalanet.server.core.draw.application.query.model.GetPublicDrawResultQuery;
import com.tchalanet.server.core.draw.infra.web.model.PublicDrawResultItemResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetPublicDrawResultQueryHandler
    implements QueryHandler<GetPublicDrawResultQuery, PublicDrawResultItemResponse> {

  private final PublicDrawResultPort port;
  private final JsonUtils jsonUtils;
  private final DrawChannelLabelResolver channelLabelResolver;

  @Override
  public PublicDrawResultItemResponse handle(GetPublicDrawResultQuery query) {
    return port.findOne(query.channelCode(), query.drawDate())
        .map(
            row -> {
              String channelLabel =
                  channelLabelResolver.resolve(
                      row.getChannelName(),
                      row.getChannelDrawTime(),
                      java.util.Locale.getDefault());

              return new PublicDrawResultItemResponse(
                  row.getChannelCode(),
                  channelLabel,
                  row.getDrawDate(),
                  row.getOccurredAt(),
                  mapNumbers(row.getNumbersMainJson()),
                  mapNumbers(row.getNumbersExtraJson()),
                  row.getQuality(),
                  row.getSource());
            })
        .orElseThrow(() -> new EntityNotFoundException("Draw result not found"));
  }

  private List<Integer> mapNumbers(String jsonb) {
    if (jsonb == null || jsonb.isEmpty()) return Collections.emptyList();
    try {
      return jsonUtils.readValue(jsonb, new TypeReference<List<Integer>>() {});
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }
}
