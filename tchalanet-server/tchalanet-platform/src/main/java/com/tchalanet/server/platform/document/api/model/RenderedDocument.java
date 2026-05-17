package com.tchalanet.server.platform.document.api.model;

import java.util.Objects;

public record RenderedDocument(
    byte[] bytes,
    String contentType,
    String filename,
    DocumentFormat format
) {

    public RenderedDocument {
        Objects.requireNonNull(bytes, "bytes is required");
        Objects.requireNonNull(contentType, "contentType is required");
        Objects.requireNonNull(filename, "filename is required");
        Objects.requireNonNull(format, "format is required");

        bytes = bytes.clone();

        if (filename.isBlank()) {
            throw new IllegalArgumentException("filename is required");
        }
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }

    public static RenderedDocument of(byte[] bytes, DocumentFormat format, String filename) {
        Objects.requireNonNull(format, "format is required");
        return new RenderedDocument(bytes, format.contentType(), filename, format);
    }
}
