package com.tchalanet.server.platform.communication.internal.adapter;

import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryProviderRegistry {

  private final List<DeliveryProvider> providers;

  public DeliveryProvider providerFor(CommunicationChannel channel) {
    return providers.stream()
        .filter(provider -> provider.supports(channel))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No communication provider for channel " + channel));
  }
}
