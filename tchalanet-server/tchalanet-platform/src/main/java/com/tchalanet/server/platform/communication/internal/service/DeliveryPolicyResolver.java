package com.tchalanet.server.platform.communication.internal.service;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.internal.persistence.CommunicationSettingsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryPolicyResolver {

  private final CommunicationSettingsJpaRepository settings;

  public boolean canCreateOutboundMessage(SendOutboundMessageRequest request) {
    if (request.channel() != CommunicationChannel.SLACK_TENANT_WEBHOOK) {
      return true;
    }

    if (request.recipient() == null || request.recipient().tenantId() == null) {
      return false;
    }

    return settings.findByTenantId(request.recipient().tenantId().value())
        .map(setting -> setting.isTenantSlackEnabled())
        .orElse(false);
  }
}
