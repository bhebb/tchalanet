package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.core.notification.application.port.out.NotificationReaderPort;
import com.tchalanet.server.core.notification.application.query.model.ListNotificationDeliveriesQuery;
import com.tchalanet.server.core.notification.application.query.model.NotificationDeliveryView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListNotificationDeliveriesQueryHandler
    implements QueryHandler<ListNotificationDeliveriesQuery, TchPage<NotificationDeliveryView>> {

  private final NotificationReaderPort reader;

  @Override
  public TchPage<NotificationDeliveryView> handle(ListNotificationDeliveriesQuery query) {
    return reader.listDeliveries(
        query.notificationId().map(id -> id.value()), query.status(), query.pageRequest());
  }
}
