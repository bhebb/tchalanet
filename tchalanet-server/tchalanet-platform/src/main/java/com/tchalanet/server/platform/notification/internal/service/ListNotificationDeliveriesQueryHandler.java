package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.platform.notification.internal.service.NotificationReaderPort;
import com.tchalanet.server.platform.notification.api.model.ListNotificationDeliveriesQuery;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryView;
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
