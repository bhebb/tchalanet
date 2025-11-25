package com.tchalanet.server.external.ports;

import java.util.Map;

public interface SearchIndexPort {
  void index(String indexName, Map<String, Object> document);

  void delete(String indexName, String id);
}
