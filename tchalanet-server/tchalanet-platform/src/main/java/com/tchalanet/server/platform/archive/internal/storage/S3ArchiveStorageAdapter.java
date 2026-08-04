package com.tchalanet.server.platform.archive.internal.storage;

import com.tchalanet.server.platform.archive.internal.config.ArchiveProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Stores archive objects on an S3-compatible endpoint — Cloudflare R2 in production.
 *
 * <p>"S3" names the protocol, not the provider: no AWS account is involved. The bucket is
 * deliberately separate from the database-backup bucket so that retention policies and credentials
 * stay independent, and an archive purge can never reach the backups.
 */
@Slf4j
public class S3ArchiveStorageAdapter implements ArchiveStoragePort {

  private final ArchiveProperties props;
  private final S3Client s3;

  public S3ArchiveStorageAdapter(ArchiveProperties props, S3Client s3) {
    this.props = props;
    this.s3 = s3;
  }

  /**
   * Buffers to a temporary file and uploads on close.
   *
   * <p>The port hands callers an {@link OutputStream}, but S3 needs the content length up front.
   * Archive objects target 512 MB compressed, so buffering to disk — not memory — is the honest
   * trade: bounded heap, one extra local write, and a single well-formed upload. A failed upload
   * leaves no partial object behind, which matters because the run executor treats a present object
   * as evidence that data is safe to purge.
   */
  @Override
  public OutputStream openWrite(String uri) {
    try {
      Path temp = Files.createTempFile("tch-archive-", ".tmp");
      return new java.io.BufferedOutputStream(Files.newOutputStream(temp)) {
        private boolean closed = false;

        @Override
        public void close() throws IOException {
          if (closed) {
            return;
          }
          closed = true;
          try {
            super.close();
            s3.putObject(
                PutObjectRequest.builder().bucket(props.storage().bucket()).key(uri).build(),
                RequestBody.fromFile(temp));
            log.debug("archive storage: uploaded {} ({} bytes)", uri, Files.size(temp));
          } finally {
            Files.deleteIfExists(temp);
          }
        }
      };
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to open write stream: " + uri, e);
    }
  }

  @Override
  public InputStream openRead(String uri) {
    return s3.getObject(
        GetObjectRequest.builder().bucket(props.storage().bucket()).key(uri).build());
  }

  @Override
  public boolean exists(String uri) {
    try {
      s3.headObject(HeadObjectRequest.builder().bucket(props.storage().bucket()).key(uri).build());
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    }
  }

  @Override
  public long size(String uri) {
    return s3.headObject(
            HeadObjectRequest.builder().bucket(props.storage().bucket()).key(uri).build())
        .contentLength();
  }

  @Override
  public void delete(String uri) {
    try {
      s3.deleteObject(
          DeleteObjectRequest.builder().bucket(props.storage().bucket()).key(uri).build());
    } catch (RuntimeException e) {
      log.warn("archive storage: failed to delete {}: {}", uri, e.getMessage());
    }
  }

  /** Same layout as the local adapter, so objects stay portable between the two. */
  @Override
  public String buildUri(String tableName, String tenantId, int year, int month, String segmentId) {
    return "%s/%s/%s/%04d/%02d/%s.jsonl.gz"
        .formatted(props.storage().prefix(), tableName, tenantId, year, month, segmentId);
  }
}
