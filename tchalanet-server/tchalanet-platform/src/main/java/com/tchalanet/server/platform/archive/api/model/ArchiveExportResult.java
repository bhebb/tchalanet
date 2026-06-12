package com.tchalanet.server.platform.archive.api.model;

/**
 * Outcome of an archive export segment.
 *
 * <p>Providers return the 2-arg form (rows + schemaVersion).
 * The executor enriches the result with the SHA-256 checksum computed from the
 * {@link com.tchalanet.server.platform.archive.internal.io.JsonlGzWriter} after close.
 */
public record ArchiveExportResult(long rowsExported, int schemaVersion, String checksumSha256) {

  /** Convenience constructor for providers that do not compute the checksum. */
  public ArchiveExportResult(long rowsExported, int schemaVersion) {
    this(rowsExported, schemaVersion, null);
  }
}
