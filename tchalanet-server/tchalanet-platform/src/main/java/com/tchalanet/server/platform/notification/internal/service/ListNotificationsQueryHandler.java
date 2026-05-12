package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.platform.notification.internal.service.NotificationReaderPort;
import com.tchalanet.server.platform.notification.api.model.ListNotificationsQuery;
import com.tchalanet.server.platform.notification.api.model.NotificationItemView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListNotificationsQueryHandler
    implements QueryHandler<ListNotificationsQuery, TchPage<NotificationItemView>> {

  private final NotificationReaderPort reader;

  @Override
  public TchPage<NotificationItemView> handle(ListNotificationsQuery query) {
    return reader.list(
        query.userId(),
        query.roleCode(),
        query.status(),
        query.category(),
        query.kind(),
        query.severity(),
        query.pageRequest());
  }
}
