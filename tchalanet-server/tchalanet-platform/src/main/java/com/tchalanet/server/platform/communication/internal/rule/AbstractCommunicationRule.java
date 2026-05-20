package com.tchalanet.server.platform.communication.internal.rule;

import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class AbstractCommunicationRule<T> implements CommunicationRule<T> {

  protected Map<String, Object> metadata(String templateKey, String correlationKey, String title, String message) {
    var metadata = new LinkedHashMap<String, Object>();
    metadata.put("templateKey", templateKey);
    metadata.put("correlationKey", correlationKey);
    metadata.put("title", title);
    metadata.put("message", message);
    return metadata;
  }

  protected CommunicationChannel internalSlack() {
    return CommunicationChannel.SLACK_INTERNAL;
  }
}
