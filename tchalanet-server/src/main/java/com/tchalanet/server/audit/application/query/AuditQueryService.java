package com.tchalanet.server.audit.application.query;

import java.util.List;
import java.util.Map;

public interface AuditQueryService {
  List<Map<String, Object>> findAuditLogs(String entity, String action, String id, String details);
}
