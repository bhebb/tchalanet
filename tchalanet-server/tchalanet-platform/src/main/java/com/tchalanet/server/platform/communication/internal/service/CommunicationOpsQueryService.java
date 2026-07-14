package com.tchalanet.server.platform.communication.internal.service;

import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.DeliveryStatus;
import com.tchalanet.server.platform.communication.internal.persistence.MessageDeliveryAttemptJpaEntity;
import com.tchalanet.server.platform.communication.internal.persistence.MessageDeliveryAttemptJpaRepository;
import com.tchalanet.server.platform.communication.internal.persistence.OutboundMessageJpaEntity;
import com.tchalanet.server.platform.communication.internal.persistence.OutboundMessageJpaRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunicationOpsQueryService {

  private final OutboundMessageJpaRepository messages;
  private final MessageDeliveryAttemptJpaRepository attempts;

  public QueueSnapshot searchMessages(
      DeliveryStatus status,
      CommunicationChannel channel,
      UUID tenantId,
      String recipientPattern,
      Pageable pageable) {
    var page = messages.searchOpsMessages(status, channel, tenantId, recipientPattern, pageable);
    var messageIds = page.getContent().stream().map(OutboundMessageJpaEntity::getId).toList();
    var attemptsByMessage =
        messageIds.isEmpty()
            ? Map.<UUID, List<MessageDeliveryAttemptJpaEntity>>of()
            : attempts
                .findRecentForMessages(
                    messageIds, PageRequest.of(0, Math.max(20, page.getContent().size() * 3)))
                .stream()
                .collect(Collectors.groupingBy(MessageDeliveryAttemptJpaEntity::getMessageId));
    return new QueueSnapshot(page, attemptsByMessage, summary());
  }

  private QueueSummary summary() {
    return new QueueSummary(
        messages.countByDeletedAtIsNullAndStatus(DeliveryStatus.PENDING),
        messages.countByDeletedAtIsNullAndStatus(DeliveryStatus.DISPATCHING),
        messages.countByDeletedAtIsNullAndStatus(DeliveryStatus.SENT),
        messages.countByDeletedAtIsNullAndStatus(DeliveryStatus.FAILED),
        messages.countByDeletedAtIsNullAndStatus(DeliveryStatus.SKIPPED),
        messages.countByDeletedAtIsNullAndStatus(DeliveryStatus.CANCELLED));
  }

  public record QueueSnapshot(
      Page<OutboundMessageJpaEntity> messages,
      Map<UUID, List<MessageDeliveryAttemptJpaEntity>> attemptsByMessage,
      QueueSummary summary) {}

  public record QueueSummary(
      long pending, long dispatching, long sent, long failed, long skipped, long cancelled) {}
}
