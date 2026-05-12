package com.tchalanet.server.platform.notification.internal.service;


public interface NotificationDeliveryWriterPort {
  NotificationDelivery save(NotificationDelivery delivery);
}
