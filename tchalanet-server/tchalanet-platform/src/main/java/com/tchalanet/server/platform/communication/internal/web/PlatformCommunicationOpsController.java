package com.tchalanet.server.platform.communication.internal.web;

import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
// The servlet path already prefixes /api/v1 (spring.mvc.servlet.path); do not repeat it here
// or the endpoint resolves to /api/v1/api/v1/... and 404s.
@RequestMapping("/platform/ops/communication")
@Tag(name = "Platform Ops • Communication")
@RequiredArgsConstructor
public class PlatformCommunicationOpsController {

  private final CommunicationApi communicationApi;

  @PostMapping("/slack-test")
  @PreAuthorize("hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CommunicationTestResponse>> testSlack(
      @Valid @RequestBody SlackTestRequest request) {
    var result = communicationApi.sendNow(new SendOutboundMessageRequest(
        "OPS_SLACK_TEST",
        CommunicationChannel.SLACK_INTERNAL,
        OutboundRecipient.slack(request.channelKey()),
        Locale.ENGLISH,
        Map.of(
            "title", request.title(),
            "message", request.message(),
            "severity", "INFO",
            "requestId", "ops-slack-test")));

    return response("SLACK_INTERNAL", result);
  }

  @PostMapping("/email-test")
  @PreAuthorize("hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<CommunicationTestResponse>> testEmail(
      @Valid @RequestBody EmailTestRequest request) {
    var result = communicationApi.sendNow(new SendOutboundMessageRequest(
        "OPS_EMAIL_TEST",
        CommunicationChannel.EMAIL,
        new OutboundRecipient(null, null, request.to(), null),
        Locale.ENGLISH,
        Map.of(
            "title", request.subject(),
            "subject", request.subject(),
            "message", request.message(),
            "body", request.message(),
            "requestId", "ops-email-test")));

    return response("EMAIL", result);
  }

  private ResponseEntity<ApiResponse<CommunicationTestResponse>> response(
      String channel,
      SendOutboundMessageResult result) {
    var body = new CommunicationTestResponse(result.sent(), result.provider(), result.reason(), channel);
    if (result.sent()) {
      return ResponseEntity.ok(ApiResponse.success(body));
    }

    return ResponseEntity.ok(ApiResponse.warn(
        body,
        ApiNotice.warn("COMMUNICATION_TEST_DEGRADED", result.reason())));
  }

  public record SlackTestRequest(
      @NotBlank String channelKey,
      @NotBlank String title,
      @NotBlank String message) {}

  public record EmailTestRequest(
      @Email @NotBlank String to,
      @NotBlank String subject,
      @NotBlank String message) {}

  public record CommunicationTestResponse(
      boolean sent,
      String provider,
      String reason,
      String channel) {}
}
