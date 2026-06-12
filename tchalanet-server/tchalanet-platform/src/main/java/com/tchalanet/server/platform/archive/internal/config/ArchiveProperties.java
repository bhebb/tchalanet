package com.tchalanet.server.platform.archive.internal.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "tch.archive")
public record ArchiveProperties(
    @DefaultValue("false") boolean enabled,
    @DefaultValue Storage storage,
    @DefaultValue Restore restore,
    @DefaultValue Cleanup cleanup
) {

  public record Storage(
      @DefaultValue("local") String type,
      @DefaultValue("./archive-data") String localRoot,
      @DefaultValue("tchalanet-archive") String bucket,
      @DefaultValue("archive") String prefix,
      @DefaultValue("536870912") long targetCompressedObjectBytes  // 512 MB
  ) {}

  public record Restore(
      @DefaultValue("P7D") Duration tempTtl,
      @DefaultValue("1000000") long maxRowsPerRun,
      @DefaultValue("5") int maxActiveRestoreRuns
  ) {}

  /** Partition DDL cleanup is disabled by default; DRY_RUN produces a plan without DDL. */
  public record Cleanup(
      @DefaultValue("false") boolean enabled,
      @DefaultValue("DRY_RUN") String mode
  ) {}
}
