package com.tchalanet.server.common.contracts.results;

import com.tchalanet.server.common.types.enums.ResultQuality;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record SourceFlags(
    IngestionMode mode,
    String provider,
    ExternalFetchStatus fetchStatus,
    ResultQuality providerQuality,
    String origin,
    String hash,
    String queryHash,
    Instant fetchedAt,
    Map<String, Object> extra) {

  public SourceFlags {
    Objects.requireNonNull(mode, "mode required");
    if (provider == null) provider = "";
    if (fetchStatus == null) fetchStatus = ExternalFetchStatus.NOT_FOUND;
    if (providerQuality == null) providerQuality = ResultQuality.SUSPECT;
    if (origin == null) origin = "";
    if (hash == null) hash = "";
    if (queryHash == null) queryHash = "";
    if (fetchedAt == null) fetchedAt = Instant.EPOCH;
    extra = (extra == null) ? Map.of() : Map.copyOf(extra);
  }

  public static SourceFlags manual(String provider, String recordedBy) {
    return new SourceFlags(
        IngestionMode.MANUAL,
        provider,
        ExternalFetchStatus.FOUND,
        ResultQuality.COMPLETE,
        "MANUAL",
        "",
        "",
        Instant.now(),
        Map.of("recorded_by", recordedBy));
  }
}
