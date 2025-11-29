package com.tchalanet.server.draw.application.port.out;

import com.tchalanet.server.draw.domain.model.Draw;
import java.util.List;

public interface DrawWriterPort {
  Draw save(Draw draw);

  List<Draw> saveAll(List<Draw> draws);

  List<Draw> updateDraws(List<Draw> draws);
}
