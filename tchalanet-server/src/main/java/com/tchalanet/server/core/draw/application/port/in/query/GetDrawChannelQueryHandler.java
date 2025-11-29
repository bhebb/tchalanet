package com.tchalanet.server.core.draw.application.port.in.query;

import com.tchalanet.server.core.draw.application.query.model.GetDrawChannelQuery;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;

public interface GetDrawChannelQueryHandler {
  DrawChannel get(GetDrawChannelQuery query);
}
