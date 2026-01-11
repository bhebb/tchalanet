package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.core.draw.domain.model.DrawChannel;

public interface DrawChannelWriterPort {
  DrawChannel save(DrawChannel channel);
}
