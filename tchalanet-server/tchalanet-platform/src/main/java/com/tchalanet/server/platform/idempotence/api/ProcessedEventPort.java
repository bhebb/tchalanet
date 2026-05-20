package com.tchalanet.server.platform.idempotence.api;

import java.util.UUID;

public interface ProcessedEventPort {

  boolean alreadyProcessed(String handlerKey, UUID eventId);

  void markProcessed(String handlerKey, UUID eventId);

  boolean markProcessedIfAbsent(String handlerKey, UUID eventId);
}
