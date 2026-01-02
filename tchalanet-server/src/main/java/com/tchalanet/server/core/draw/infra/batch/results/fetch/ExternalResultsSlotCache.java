package com.tchalanet.server.core.draw.infra.batch.results.fetch;

import com.tchalanet.server.core.draw.application.port.out.ExternalDrawResultPort;
import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@Component
@StepScope
@NoArgsConstructor
public class ExternalResultsSlotCache {

  private final Map<String, ExternalDrawResultPort.ExternalDrawResult> byChannel = new HashMap<>();
  private boolean loaded = false;

  public boolean isLoaded() {
    return loaded;
  }

  public void markLoaded() {
    this.loaded = true;
  }

  public void putAll(Map<String, ExternalDrawResultPort.ExternalDrawResult> m) {
    if (m == null) return;
    byChannel.putAll(m);
  }

  public ExternalDrawResultPort.ExternalDrawResult get(String channelCode) {
    if (channelCode == null) return null;
    return byChannel.get(channelCode.trim().toUpperCase());
  }
}

