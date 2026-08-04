package com.tchalanet.server.platform.archive.internal.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "tch.archive")
public record ArchiveProperties(
    @DefaultValue("false") boolean enabled,
    @DefaultValue Storage storage,
    @DefaultValue Restore restore,
    @DefaultValue Cleanup cleanup) {

  /**
   * Object storage settings.
   *
   * <p>{@code type=s3} targets any S3-compatible endpoint. In production this is Cloudflare R2 — no
   * AWS account is involved, "S3" here names the protocol rather than the provider. Archives must
   * not share the backup bucket: separate buckets keep retention and credentials independent, so an
   * archive purge can never reach the database backups.
   */
  public record Storage(
      @DefaultValue("local") String type,
      @DefaultValue("./archive-data") String localRoot,
      @DefaultValue("tch-archive") String bucket,
      @DefaultValue("archive") String prefix,
      @DefaultValue("536870912") long targetCompressedObjectBytes, // 512 MB
      String endpoint,
      @DefaultValue("auto") String region,
      String accessKeyId,
      String secretAccessKey) {}

  public record Restore(
      @DefaultValue("P7D") Duration tempTtl,
      @DefaultValue("1000000") long maxRowsPerRun,
      @DefaultValue("5") int maxActiveRestoreRuns) {}

  /** Partition DDL cleanup is disabled by default; DRY_RUN produces a plan without DDL. */
  public record Cleanup(
      @DefaultValue("false") boolean enabled,
      @DefaultValue("DRY_RUN") String mode,
      @DefaultValue("12") int retentionMonths,
      @DefaultValue("audit_log") List<String> cleanableTables) {}
}
