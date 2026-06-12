package com.tchalanet.server.platform.archive.api.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generic view for an archived entity retrieved from object storage.
 *
 * <p>Callers that need typed DTOs (e.g., archived ticket) deserialize
 * {@code payload} with the appropriate schema version.
 */
public record ArchivedEntityView(
    boolean found,
    UUID entityId,
    String publicCode,
    String tableName,
    int schemaVersion,
    String objectUri,
    List<Map<String, Object>> rows
) {

  public static ArchivedEntityView notFound(UUID entityId) {
    return new ArchivedEntityView(false, entityId, null, null, 0, null, List.of());
  }
}
