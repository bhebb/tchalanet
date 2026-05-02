package com.tchalanet.server.core.drawresult.application.port.out.external;

import java.util.Map;

public record ExternalSourceFlags(
    String origin,
    String sourceHash,
    String url,
    Map<String, String> metadata
) {
    public ExternalSourceFlags {
        origin = origin == null ? "" : origin;
        sourceHash = sourceHash == null ? "" : sourceHash;
        url = url == null ? "" : url;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static ExternalSourceFlags manual(String recordedBy) {
        return new ExternalSourceFlags(
            "MANUAL",
            "",
            "",
            Map.of("recorded_by", recordedBy == null ? "" : recordedBy));
    }

    public static ExternalSourceFlags override(String reason) {
        return new ExternalSourceFlags(
            "OVERRIDE",
            "",
            "",
            Map.of("reason", reason == null ? "" : reason));
    }
}
