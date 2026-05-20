package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryStatus;
import com.tchalanet.server.platform.notification.api.model.view.NotificationDeliveryView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationItemView;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import com.tchalanet.server.platform.notification.internal.mapper.NotificationMapper;
import com.tchalanet.server.platform.notification.internal.service.Notification;
import com.tchalanet.server.platform.notification.internal.service.NotificationDelivery;
import com.tchalanet.server.platform.notification.internal.service.NotificationDeliveryWriterPort;
import com.tchalanet.server.platform.notification.internal.service.NotificationReaderPort;
import com.tchalanet.server.platform.notification.internal.service.NotificationWriterPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter
    implements NotificationWriterPort, NotificationReaderPort, NotificationDeliveryWriterPort {

  private final Clock clock;
  private final NotificationJpaRepository notifications;
  private final NotificationDeliveryJpaRepository deliveries;

  @Override
  public Optional<Notification> findByDedupeKey(String dedupeKey) {
    if (dedupeKey == null || dedupeKey.isBlank()) {
      return Optional.empty();
    }
    return notifications.findFirstByDedupeKeyAndDeletedAtIsNull(dedupeKey).map(NotificationMapper::toDomain);
  }

  @Override
  public Notification save(Notification notification) {
    return NotificationMapper.toDomain(
        notifications.save(NotificationMapper.toEntity(notification, null)));
  }

  @Override
  public void markRead(NotificationId id, Instant readAt) {
    notifications.markRead(id.value(), readAt);
  }

  @Override
  public void markRead(List<NotificationId> ids, Instant readAt) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    notifications.markReadAll(ids.stream().map(NotificationId::value).toList(), readAt);
  }

  @Override
  public void archive(NotificationId id, Instant archivedAt) {
    notifications.archive(id.value(), archivedAt);
  }

  @Override
  public void archive(List<NotificationId> ids, Instant archivedAt) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    notifications.archiveAll(ids.stream().map(NotificationId::value).toList(), archivedAt);
  }

  @Override
  public int expire(Instant now) {
    return notifications.expire(now);
  }

  @Override
  public NotificationSummaryView summary(UserId userId, String roleCode) {
    var now = clock.instant();
    var userValue = userId == null ? null : userId.value().toString();
    var unread =
        notifications.countVisible(
            userValue, roleCode, NotificationStatus.UNREAD, null, null, now);
    var critical =
        notifications.countVisible(
            userValue, roleCode, NotificationStatus.UNREAD, null, NotificationSeverity.CRITICAL, now);
    var actionRequired =
        notifications.countVisible(
            userValue, roleCode, NotificationStatus.UNREAD, NotificationKind.ACTION_REQUIRED, null, now);
    return new NotificationSummaryView(unread, critical, actionRequired, actionRequired > 0);
  }

  @Override
  public TchPage<NotificationItemView> list(
      UserId userId,
      String roleCode,
      Optional<NotificationStatus> status,
      Optional<NotificationCategory> category,
      Optional<NotificationKind> kind,
      Optional<NotificationSeverity> severity,
      TchPageRequest pageRequest) {
    var userValue = userId == null ? null : userId.value().toString();
    var page =
        notifications.searchVisible(
            userValue,
            roleCode,
            status.orElse(null),
            category.orElse(null),
            kind.orElse(null),
            severity.orElse(null),
            clock.instant(),
            pageRequest.pageable());
    return TchPageMapper.map(page, NotificationMapper::toItemView);
  }

  @Override
  public TchPage<NotificationDeliveryView> listDeliveries(
      Optional<UUID> notificationId,
      Optional<NotificationDeliveryStatus> status,
      TchPageRequest pageRequest) {
    var page = deliveries.search(notificationId.orElse(null), status.orElse(null), pageRequest.pageable());
    return TchPageMapper.map(page, NotificationMapper::toDeliveryView);
  }

  @Override
  public NotificationDelivery save(NotificationDelivery delivery) {
    return NotificationMapper.toDomain(deliveries.save(NotificationMapper.toEntity(delivery, null)));
  }
}
