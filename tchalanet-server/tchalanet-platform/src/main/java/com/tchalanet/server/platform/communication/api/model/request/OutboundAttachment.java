package com.tchalanet.server.platform.communication.api.model.request;

import java.util.Base64;
import java.util.Map;

public record OutboundAttachment(
    String filename,
    String contentType,
    byte[] content
) {
    public OutboundAttachment {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("attachment filename is required");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("attachment contentType is required");
        }
        if (content == null) {
            throw new IllegalArgumentException("attachment content is required");
        }
        filename = filename.trim();
        contentType = contentType.trim();
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public Map<String, Object> toMetadata() {
        return Map.of(
            "filename", filename,
            "contentType", contentType,
            "contentBase64", Base64.getEncoder().encodeToString(content)
        );
    }

    @SuppressWarnings("unchecked")
    public static OutboundAttachment fromMetadata(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("attachment metadata must be an object");
        }
        var filename = String.valueOf(map.get("filename"));
        var contentType = String.valueOf(map.get("contentType"));
        var contentBase64 = String.valueOf(map.get("contentBase64"));
        return new OutboundAttachment(
            filename,
            contentType,
            Base64.getDecoder().decode(contentBase64)
        );
    }
}
