package com.tchalanet.server.platform.communication.internal.web;

import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.DeliveryStatus;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import com.tchalanet.server.platform.communication.internal.persistence.MessageDeliveryAttemptJpaEntity;
import com.tchalanet.server.platform.communication.internal.persistence.MessageDeliveryAttemptJpaRepository;
import com.tchalanet.server.platform.communication.internal.persistence.OutboundMessageJpaEntity;
import com.tchalanet.server.platform.communication.internal.persistence.OutboundMessageJpaRepository;
import com.tchalanet.server.platform.communication.internal.service.OutboundMessageDispatcher;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/ops/communication")
@Tag(name = "Platform Ops • Communication")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformCommunicationOpsController {

  private final CommunicationApi communicationApi;
  private final OutboundMessageJpaRepository messages;
  private final MessageDeliveryAttemptJpaRepository attempts;
  private final OutboundMessageDispatcher dispatcher;

  @GetMapping("/messages")
  public ApiResponse<CommunicationQueueView> listMessages(
      @RequestParam(required = false) DeliveryStatus status,
      @RequestParam(required = false) CommunicationChannel channel,
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) String recipient,
      @TchPaging(
          allowedSort = {"createdAt", "status", "channel", "nextAttemptAt", "sentAt", "failedAt"},
          defaultSort = {"createdAt,DESC"})
          TchPageRequest pageReq) {
    var page =
        messages.searchOpsMessages(
            status, channel, tenantId, recipientPattern(recipient), pageReq.pageable());
    var messageIds = page.getContent().stream().map(OutboundMessageJpaEntity::getId).toList();
    var attemptsByMessage = messageIds.isEmpty()
        ? Map.<UUID, List<MessageDeliveryAttemptJpaEntity>>of()
        : attempts.findRecentForMessages(messageIds, PageRequest.of(0, Math.max(20, page.getContent().size() * 3)))
            .stream()
            .collect(Collectors.groupingBy(MessageDeliveryAttemptJpaEntity::getMessageId));

    var mapped = TchPageMapper.map(page, message -> toMessageView(message, attemptsByMessage.getOrDefault(message.getId(), List.of())));
    return ApiResponse.success(new CommunicationQueueView(summary(), mapped));
  }

  @PostMapping("/dispatch-due")
  public ApiResponse<CommunicationDispatchResult> dispatchDueMessages() {
    return ApiResponse.success(new CommunicationDispatchResult(dispatcher.dispatchDueMessages()));
  }

  @PostMapping("/slack-test")
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

  @PostMapping("/sms-test")
  public ResponseEntity<ApiResponse<CommunicationTestResponse>> testSms(
      @Valid @RequestBody SmsTestRequest request) {
    var result = communicationApi.sendNow(new SendOutboundMessageRequest(
        "OPS_SMS_TEST",
        CommunicationChannel.SMS,
        new OutboundRecipient(null, null, request.to(), null),
        Locale.FRENCH,
        Map.of(
            "title", request.title(),
            "message", request.message(),
            "severity", "INFO",
            "requestId", "ops-sms-test")));

    return response("SMS", result);
  }

  @PostMapping("/whatsapp-test")
  public ResponseEntity<ApiResponse<CommunicationTestResponse>> testWhatsapp(
      @Valid @RequestBody SmsTestRequest request) {
    var result = communicationApi.sendNow(new SendOutboundMessageRequest(
        "OPS_WHATSAPP_TEST",
        CommunicationChannel.WHATSAPP,
        new OutboundRecipient(null, null, request.to(), null),
        Locale.FRENCH,
        Map.of(
            "title", request.title(),
            "message", request.message(),
            "severity", "INFO",
            "requestId", "ops-whatsapp-test")));

    return response("WHATSAPP", result);
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

  private CommunicationQueueSummary summary() {
    return new CommunicationQueueSummary(
        messages.countByDeletedAtIsNullAndStatus(DeliveryStatus.PENDING),
        messages.countByDeletedAtIsNullAndStatus(DeliveryStatus.DISPATCHING),
        messages.countByDeletedAtIsNullAndStatus(DeliveryStatus.SENT),
        messages.countByDeletedAtIsNullAndStatus(DeliveryStatus.FAILED),
        messages.countByDeletedAtIsNullAndStatus(DeliveryStatus.SKIPPED),
        messages.countByDeletedAtIsNullAndStatus(DeliveryStatus.CANCELLED));
  }

  private CommunicationMessageView toMessageView(
      OutboundMessageJpaEntity message,
      List<MessageDeliveryAttemptJpaEntity> rawAttempts) {
    var recentAttempts = rawAttempts.stream()
        .sorted(Comparator.comparing(MessageDeliveryAttemptJpaEntity::getAttemptedAt).reversed())
        .limit(3)
        .map(this::toAttemptView)
        .toList();
    return new CommunicationMessageView(
        message.getId(),
        message.getTenantId(),
        message.getChannel(),
        message.getRecipientType(),
        message.getRecipientValue(),
        message.getTemplateKey(),
        message.getLocale(),
        message.getSubject(),
        message.getPriority().name(),
        message.getStatus(),
        message.getCorrelationKey(),
        message.getNextAttemptAt(),
        message.getSentAt(),
        message.getFailedAt(),
        message.getFailureReason(),
        message.getCreatedAt(),
        recentAttempts);
  }

  private CommunicationAttemptView toAttemptView(MessageDeliveryAttemptJpaEntity attempt) {
    return new CommunicationAttemptView(
        attempt.getId(),
        attempt.getAttemptedAt(),
        attempt.getStatus(),
        attempt.getProvider(),
        attempt.getProviderMessageId(),
        attempt.getErrorCode(),
        attempt.getErrorMessage());
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String recipientPattern(String value) {
    var normalized = blankToNull(value);
    return normalized == null ? null : "%" + normalized.toLowerCase(Locale.ROOT) + "%";
  }

  public record SlackTestRequest(
      @NotBlank String channelKey,
      @NotBlank String title,
      @NotBlank String message) {}

  public record EmailTestRequest(
      @Email @NotBlank String to,
      @NotBlank String subject,
      @NotBlank String message) {}

  public record SmsTestRequest(
      @NotBlank String to,
      @NotBlank String title,
      @NotBlank String message) {}

  public record CommunicationTestResponse(
      boolean sent,
      String provider,
      String reason,
      String channel) {}

  public record CommunicationQueueView(
      CommunicationQueueSummary summary,
      TchPage<CommunicationMessageView> messages) {}

  public record CommunicationQueueSummary(
      long pending,
      long dispatching,
      long sent,
      long failed,
      long skipped,
      long cancelled) {}

  public record CommunicationMessageView(
      UUID id,
      UUID tenantId,
      CommunicationChannel channel,
      String recipientType,
      String recipientValue,
      String templateKey,
      String locale,
      String subject,
      String priority,
      DeliveryStatus status,
      String correlationKey,
      Instant nextAttemptAt,
      Instant sentAt,
      Instant failedAt,
      String failureReason,
      Instant createdAt,
      List<CommunicationAttemptView> attempts) {}

  public record CommunicationAttemptView(
      UUID id,
      Instant attemptedAt,
      DeliveryStatus status,
      String provider,
      String providerMessageId,
      String errorCode,
      String errorMessage) {}

  public record CommunicationDispatchResult(int dispatched) {}
}
