package com.tchalanet.server.draw.application.port.in.query;

import com.tchalanet.server.draw.application.query.model.GetDrawChannelQuery;
import com.tchalanet.server.draw.domain.model.DrawChannel;

public interface GetDrawChannelQueryHandler {
  DrawChannel get(GetDrawChannelQuery query);
}
