package com.tchalanet.server.platform.notification.internal.rule;

import java.util.stream.Stream;

public interface NotificationRule {

  String handlerKey();

  boolean supports(Object event);

  Stream<NotificationIntent> map(Object event);
}
