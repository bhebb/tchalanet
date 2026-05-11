package com.tchalanet.server.core.offlinesync.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfflinePayloadParser {

  private final ObjectMapper objectMapper;

  public JsonNode parse(String payloadJson) {
    try {
      return objectMapper.readTree(payloadJson);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid offline payload JSON", ex);
    }
  }
}

