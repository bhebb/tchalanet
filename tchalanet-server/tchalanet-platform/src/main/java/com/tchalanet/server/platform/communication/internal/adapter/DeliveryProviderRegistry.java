package com.tchalanet.server.platform.communication.internal.adapter;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.communication.api.error.CommunicationErrorCodes;
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
        .orElseThrow(() -> ProblemRest.of(CommunicationErrorCodes.PROVIDER_UNAVAILABLE));
  }
}
