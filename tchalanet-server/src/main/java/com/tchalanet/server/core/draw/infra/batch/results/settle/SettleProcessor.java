package com.tchalanet.server.core.draw.infra.batch.results.settle;

import java.util.UUID;
import lombok.RequiredArgsConstructor;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettleProcessor implements ItemProcessor<UUID, UUID> {

  @Override
  public UUID process(UUID item) throws Exception {
    // pass-through: turning drawId into same drawId (could build a command object)
    var drawId = item;
    return drawId;
  }
}
