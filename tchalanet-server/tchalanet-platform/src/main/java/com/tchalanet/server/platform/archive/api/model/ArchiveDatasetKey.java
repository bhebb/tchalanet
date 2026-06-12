package com.tchalanet.server.platform.archive.api.model;

/**
 * Stable identity of a dataset managed by an {@link com.tchalanet.server.platform.archive.api.ArchiveDatasetProvider}.
 *
 * <p>{@code tableName} is the physical table name and is used as the path component in
 * object-storage URIs. {@code datasetName} is a human-readable label for registry entries.
 */
public record ArchiveDatasetKey(String tableName, String datasetName) {

  public static ArchiveDatasetKey of(String tableName, String datasetName) {
    return new ArchiveDatasetKey(tableName, datasetName);
  }
}
