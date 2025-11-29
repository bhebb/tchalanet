package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.core.draw.domain.model.Draw;
import java.util.List;

public interface DrawWriterPort {
  Draw save(Draw draw);

  List<Draw> saveAll(List<Draw> draws);

  List<Draw> updateDraws(List<Draw> draws);
}
