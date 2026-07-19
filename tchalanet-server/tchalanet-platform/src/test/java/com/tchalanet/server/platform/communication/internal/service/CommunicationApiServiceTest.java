package com.tchalanet.server.platform.communication.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import com.tchalanet.server.platform.communication.internal.adapter.DeliveryProviderRegistry;
import com.tchalanet.server.platform.communication.internal.persistence.OutboundMessageJpaRepository;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CommunicationApiServiceTest {

  @Test
  void mapsPersistenceFailureToStableEnqueueCodeInsteadOfReturningNull() {
    var messages = mock(OutboundMessageJpaRepository.class);
    var rendering = mock(MessageRenderingService.class);
    var policy = mock(DeliveryPolicyResolver.class);
    var request = request();
    when(policy.canCreateOutboundMessage(request)).thenReturn(true);
    when(rendering.render(request)).thenReturn(new RenderedMessage("Subject", "Body"));
    when(messages.save(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new IllegalStateException("db down"));

    var service =
        new CommunicationApiService(
            messages,
            rendering,
            policy,
            mock(DeliveryProviderRegistry.class),
            mock(JsonUtils.class),
            Clock.systemUTC());

    assertThatThrownBy(() -> service.enqueue(request))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(
            error ->
                assertThat(((ProblemRestException) error).getProblem().getProperties())
                    .containsEntry("code", "communication.enqueue_failed"));
  }

  @Test
  void mapsMissingChannelToStableRequestCode() {
    var service =
        new CommunicationApiService(
            mock(OutboundMessageJpaRepository.class),
            mock(MessageRenderingService.class),
            mock(DeliveryPolicyResolver.class),
            mock(DeliveryProviderRegistry.class),
            mock(JsonUtils.class),
            Clock.systemUTC());

    assertThatThrownBy(() -> service.enqueue(null))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(
            error ->
                assertThat(((ProblemRestException) error).getProblem().getProperties())
                    .containsEntry("code", "communication.request_invalid"));
  }

  private SendOutboundMessageRequest request() {
    return new SendOutboundMessageRequest(
        "TEST",
        CommunicationChannel.EMAIL,
        OutboundRecipient.of("qa@example.test"),
        Locale.FRENCH,
        Map.of("subject", "Subject", "body", "Body"));
  }
}
