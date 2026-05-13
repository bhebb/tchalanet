package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.ArchiveNotificationCommand;
import com.tchalanet.server.platform.notification.api.model.CreateNotificationCommand;
import com.tchalanet.server.platform.notification.api.model.GetNotificationSummaryQuery;
import com.tchalanet.server.platform.notification.api.model.ListNotificationsQuery;
import com.tchalanet.server.platform.notification.api.model.MarkNotificationReadCommand;
import com.tchalanet.server.platform.notification.api.model.NotificationItemView;
import com.tchalanet.server.platform.notification.api.model.NotificationSummaryView;
import com.tchalanet.server.platform.notification.api.model.SendNotificationCommand;
import com.tchalanet.server.platform.notification.api.model.SendNotificationResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class DefaultNotificationApi implements NotificationApi {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @Override
  public void createNotification(CreateNotificationCommand request) {
    commandBus.execute(request);
  }

  @Override
  public SendNotificationResult sendNotification(SendNotificationCommand request) {
    return commandBus.execute(request);
  }

  @Override
  public void markRead(MarkNotificationReadCommand request) {
    commandBus.execute(request);
  }

  @Override
  public void archiveNotification(ArchiveNotificationCommand request) {
    commandBus.execute(request);
  }

  @Override
  public List<NotificationItemView> listNotifications(ListNotificationsQuery request) {
    TchPage<NotificationItemView> page = queryBus.ask(request);
    return page.items();
  }

  @Override
  public NotificationSummaryView getNotificationSummary(GetNotificationSummaryQuery request) {
    return queryBus.ask(request);
  }
}
