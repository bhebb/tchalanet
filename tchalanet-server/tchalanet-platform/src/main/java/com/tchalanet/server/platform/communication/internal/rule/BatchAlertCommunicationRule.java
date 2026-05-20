package com.tchalanet.server.platform.communication.internal.rule;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class BatchAlertCommunicationRule extends AbstractCommunicationRule<Object> {

  @Override
  public boolean supports(Object event) {
    return event != null && event.getClass().getSimpleName().equals("BatchFailedEvent");
  }

  @Override
  public Optional<SendOutboundMessageRequest> map(Object event) {
    return Optional.of(new SendOutboundMessageRequest(
        "OPS_BATCH_FAILED",
        internalSlack(),
        OutboundRecipient.slack("batch-draws"),
        Locale.ENGLISH,
        metadata("ops.batch.failed", "batch:failed", "Batch failed", event.toString())));
  }
}
