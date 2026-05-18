package com.tchalanet.server.core.drawresult.internal.application.port.out.external.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(DrawResultFetchNotificationPort.class)
public class NoopDrawResultFetchNotificationPort implements DrawResultFetchNotificationPort {
    @Override
    public void notifyFetched(DrawResultFetchNotification notification) {
        // no-op when Slack/communication is disabled
    }
}
