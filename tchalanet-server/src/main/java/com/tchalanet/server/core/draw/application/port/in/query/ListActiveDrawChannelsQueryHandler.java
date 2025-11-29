package com.tchalanet.server.core.draw.application.port.in.query;

import com.tchalanet.server.core.draw.application.query.model.ListActiveDrawChannelsQuery;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import java.util.List;

public interface ListActiveDrawChannelsQueryHandler {
  List<DrawChannelSummary> listActive(ListActiveDrawChannelsQuery query);
}
