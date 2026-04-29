package com.tchalanet.server.core.notification.application.port.out;

import com.tchalanet.server.core.notification.domain.model.NotificationDelivery;

public interface NotificationDeliveryWriterPort {
  NotificationDelivery save(NotificationDelivery delivery);
}
