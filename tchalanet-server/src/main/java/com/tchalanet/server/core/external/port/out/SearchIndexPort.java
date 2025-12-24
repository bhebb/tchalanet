package com.tchalanet.server.core.external.port.out;

import java.util.Map;

public interface SearchIndexPort {
  void index(String indexName, Map<String, Object> document);

  void delete(String indexName, String id);
}
