package com.tchalanet.server.platform.archive.internal.storage;

import com.tchalanet.server.platform.archive.internal.config.ArchiveProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes archive files to the local filesystem under {@code tch.archive.storage.local-root}.
 *
 * <p>Intended for dev and integration tests. Not suitable for production use.
 */
@Slf4j
@RequiredArgsConstructor
public class LocalFileArchiveStorageAdapter implements ArchiveStoragePort {

  private final ArchiveProperties props;

  @Override
  public OutputStream openWrite(String uri) {
    try {
      Path path = toPath(uri);
      Files.createDirectories(path.getParent());
      log.debug("archive storage: opening write -> {}", path);
      return Files.newOutputStream(path);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to open write stream: " + uri, e);
    }
  }

  @Override
  public InputStream openRead(String uri) {
    try {
      Path path = toPath(uri);
      log.debug("archive storage: opening read <- {}", path);
      return Files.newInputStream(path);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to open read stream: " + uri, e);
    }
  }

  @Override
  public boolean exists(String uri) {
    return Files.exists(toPath(uri));
  }

  @Override
  public void delete(String uri) {
    try {
      Files.deleteIfExists(toPath(uri));
    } catch (IOException e) {
      log.warn("archive storage: failed to delete {}: {}", uri, e.getMessage());
    }
  }

  @Override
  public String buildUri(String tableName, String tenantId, int year, int month, String segmentId) {
    return "%s/%s/%s/%04d/%02d/%s.jsonl.gz"
        .formatted(props.storage().prefix(), tableName, tenantId, year, month, segmentId);
  }

  private Path toPath(String uri) {
    return Path.of(props.storage().localRoot()).resolve(uri);
  }
}
