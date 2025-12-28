package com.tchalanet.server.core.draw.infra.batch.results.settle;

import com.tchalanet.server.common.types.id.DrawId;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettleProcessor implements ItemProcessor<DrawId, DrawId> {

  @Override
  public DrawId process(DrawId item) throws Exception {
    // pass-through: returning the same DrawId (could build a command object)
    return item;
  }
}
