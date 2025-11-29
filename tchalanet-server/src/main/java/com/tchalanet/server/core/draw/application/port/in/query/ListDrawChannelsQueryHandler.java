package com.tchalanet.server.core.draw.application.port.in.query;

import com.tchalanet.server.core.draw.application.query.model.ListDrawChannelsQuery;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import java.util.List;

public interface ListDrawChannelsQueryHandler {
  List<DrawChannelSummary> list(ListDrawChannelsQuery query);
}
