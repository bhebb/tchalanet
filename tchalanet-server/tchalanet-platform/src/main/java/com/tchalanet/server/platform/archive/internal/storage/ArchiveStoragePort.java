package com.tchalanet.server.platform.archive.internal.storage;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Object storage abstraction for the archive system.
 *
 * <p>Implementations: {@link LocalFileArchiveStorageAdapter} (dev/test),
 * S3-compatible adapter (production). Switched via {@code tch.archive.storage.type}.
 */
public interface ArchiveStoragePort {

  /** Open a write stream to the given URI. The caller must close the stream. */
  OutputStream openWrite(String uri);

  /** Open a read stream from the given URI. The caller must close the stream. */
  InputStream openRead(String uri);

  /** Return true if an object at {@code uri} already exists. */
  boolean exists(String uri);

  /** Return the stored object size in bytes. */
  long size(String uri);

  /** Delete the object at {@code uri}. No-op if not found. */
  void delete(String uri);

  /**
   * Build a storage URI for an archive object.
   *
   * @param tableName   physical table name
   * @param tenantId    tenant identifier string, or "global" for platform datasets
   * @param year        4-digit year
   * @param month       1-12
   * @param segmentId   unique segment identifier (UUID or sequence)
   */
  String buildUri(String tableName, String tenantId, int year, int month, String segmentId);
}
