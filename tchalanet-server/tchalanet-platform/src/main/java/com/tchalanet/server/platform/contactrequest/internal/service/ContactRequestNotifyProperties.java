package com.tchalanet.server.platform.contactrequest.internal.service;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.public-contact.notify")
public record ContactRequestNotifyProperties(
    boolean enabled,
    List<String> recipients,
    List<String> cc
) {
    public ContactRequestNotifyProperties {
        recipients = recipients != null ? List.copyOf(recipients) : List.of();
        cc = cc != null ? List.copyOf(cc) : List.of();
    }
}
