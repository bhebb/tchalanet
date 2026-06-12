package com.tchalanet.server.platform.archive.api.model;

import java.util.List;
import java.util.Map;

/**
 * Result of an archive lookup.
 *
 * <p>{@code rows} contains the deserialized payload rows. Each row is a map
 * of column name → value, matching the JSON schema version recorded at export time.
 * Callers assemble DTOs from these maps; the archive layer does not know DTO shapes.
 */
public record ArchiveLookupResult(
    boolean found,
    List<Map<String, Object>> rows,
    String objectUri,
    int schemaVersion
) {

  public static ArchiveLookupResult notFound() {
    return new ArchiveLookupResult(false, List.of(), null, 0);
  }
}
