package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.platform.notification.internal.service.NotificationReaderPort;
import com.tchalanet.server.platform.notification.api.model.GetNotificationSummaryQuery;
import com.tchalanet.server.platform.notification.api.model.NotificationSummaryView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetNotificationSummaryQueryHandler
    implements QueryHandler<GetNotificationSummaryQuery, NotificationSummaryView> {

  private final NotificationReaderPort reader;

  @Override
  public NotificationSummaryView handle(GetNotificationSummaryQuery query) {
    return reader.summary(query.userId(), query.roleCode());
  }
}
