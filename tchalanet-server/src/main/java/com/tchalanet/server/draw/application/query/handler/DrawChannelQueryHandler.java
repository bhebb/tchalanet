package com.tchalanet.server.draw.application.query.handler;

import com.tchalanet.server.draw.application.port.in.query.GetDrawChannelQueryHandler;
import com.tchalanet.server.draw.application.port.in.query.ListActiveDrawChannelsQueryHandler;
import com.tchalanet.server.draw.application.port.in.query.ListDrawChannelsQueryHandler;
import com.tchalanet.server.draw.application.port.out.DrawChannelReaderPort;
import com.tchalanet.server.draw.application.query.model.DrawChannelSearchCriteria;
import com.tchalanet.server.draw.application.query.model.GetDrawChannelQuery;
import com.tchalanet.server.draw.application.query.model.ListActiveDrawChannelsQuery;
import com.tchalanet.server.draw.application.query.model.ListDrawChannelsQuery;
import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.model.DrawChannelId;
import com.tchalanet.server.draw.domain.model.DrawChannelSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawChannelQueryHandler
    implements GetDrawChannelQueryHandler,
        ListDrawChannelsQueryHandler,
        ListActiveDrawChannelsQueryHandler {

  private final DrawChannelReaderPort drawChannelReaderPort;

  public DrawChannel get(GetDrawChannelQuery query) {
    return drawChannelReaderPort
        .findById(query.tenantId(), new DrawChannelId(query.channelId()))
        .orElseThrow(
            () -> new IllegalArgumentException("DrawChannel not found: " + query.channelId()));
  }

  public List<DrawChannelSummary> list(ListDrawChannelsQuery query) {
    var criteria = new DrawChannelSearchCriteria(query.tenantId(), query.activeOnly());
    return drawChannelReaderPort.findByCriteria(criteria);
  }

  public List<DrawChannelSummary> listActive(ListActiveDrawChannelsQuery query) {
    var criteria =
        new DrawChannelSearchCriteria(
            query.tenantId(), true // only active
            );
    return drawChannelReaderPort.findByCriteria(criteria);
  }
}
