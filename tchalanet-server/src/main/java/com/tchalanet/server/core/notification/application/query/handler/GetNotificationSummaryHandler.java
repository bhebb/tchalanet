package com.tchalanet.server.core.notification.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.notification.application.port.out.NotificationReaderPort;
import com.tchalanet.server.core.notification.application.query.model.GetNotificationSummaryQuery;
import com.tchalanet.server.core.notification.application.query.model.NotificationSummaryView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetNotificationSummaryHandler
    implements QueryHandler<GetNotificationSummaryQuery, NotificationSummaryView> {

  private final NotificationReaderPort reader;

  @Override
  public NotificationSummaryView handle(GetNotificationSummaryQuery query) {
    return reader.summary(query.userId(), query.roleCode());
  }
}
