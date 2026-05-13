package com.tchalanet.server.platform.communication.internal.adapter.sms;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.internal.adapter.DeliveryProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsProviderAdapter implements DeliveryProvider {

  @Override
  public boolean supports(CommunicationChannel channel) {
    return channel == CommunicationChannel.SMS || channel == CommunicationChannel.WHATSAPP;
  }

  @Override
  public SendOutboundMessageResult send(SendOutboundMessageRequest request) {
    log.info("DEV sms delivery skipped recipient={} type={}",
        request.recipient() == null ? null : request.recipient().to(),
        request.type());
    return SendOutboundMessageResult.skipped("dev-sms", "dev adapter");
  }
}
