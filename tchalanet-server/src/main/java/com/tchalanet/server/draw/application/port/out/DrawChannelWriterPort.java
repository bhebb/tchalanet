package com.tchalanet.server.draw.application.port.out;

import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.model.DrawChannelId;
import java.util.List;

public interface DrawChannelWriterPort {
  DrawChannel save(DrawChannel channel);

  List<DrawChannel> saveAll(List<DrawChannel> channels);

  void deleteById(DrawChannelId id);
}
