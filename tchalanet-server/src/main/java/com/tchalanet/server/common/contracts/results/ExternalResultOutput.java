package com.tchalanet.server.common.contracts.results;

import com.tchalanet.server.common.types.enums.ResultQuality;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ExternalResultOutput(
    boolean found,
    String status,
    List<String> main,
    List<String> extra,
    Instant occurredAt,
    ResultQuality quality,
    SourceFlags sourceFlags,
    Map<String, Object> rawPayload) {

  public ExternalResultOutput {
    if (status == null) status = "";
    main = (main == null) ? List.of() : List.copyOf(main);
    extra = (extra == null) ? List.of() : List.copyOf(extra);
    quality = (quality == null) ? ResultQuality.SUSPECT : quality;
    Objects.requireNonNull(sourceFlags, "sourceFlags required");
    rawPayload = (rawPayload == null) ? Map.of() : Map.copyOf(rawPayload);
  }

  public static ExternalResultOutput notFound(
      String status, SourceFlags flags, Map<String, Object> raw) {
    return new ExternalResultOutput(
        false, status, List.of(), List.of(), null, ResultQuality.SUSPECT, flags, raw);
  }

  public static ExternalResultOutput found(
      String status,
      List<String> main,
      List<String> extra,
      Instant occurredAt,
      ResultQuality quality,
      SourceFlags flags,
      Map<String, Object> raw) {
    return new ExternalResultOutput(true, status, main, extra, occurredAt, quality, flags, raw);
  }
}
