package com.tchalanet.server.platform.communication.internal.adapter.email;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.internal.adapter.DeliveryProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailProviderAdapter implements DeliveryProvider {

  @Override
  public boolean supports(CommunicationChannel channel) {
    return channel == CommunicationChannel.EMAIL;
  }

  @Override
  public SendOutboundMessageResult send(SendOutboundMessageRequest request) {
    log.info("DEV email delivery skipped recipient={} subject={} attachments={}",
        request.recipient() == null ? null : request.recipient().to(),
        request.subject(),
        request.attachments().size());
    return SendOutboundMessageResult.skipped("dev-email", "dev adapter");
  }
}
