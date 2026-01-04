package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import java.util.List;

public interface DrawChannelWriterPort {
  DrawChannel save(DrawChannel channel);

  List<DrawChannel> saveAll(List<DrawChannel> channels);

  void deleteById(DrawChannelId id);
}
