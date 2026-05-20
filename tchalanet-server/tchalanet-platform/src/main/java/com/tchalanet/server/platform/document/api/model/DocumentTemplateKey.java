package com.tchalanet.server.platform.document.api.model;

public record DocumentTemplateKey(String value) {
    public DocumentTemplateKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("template key is required");
        }
    }

    public static DocumentTemplateKey of(String value) {
        return new DocumentTemplateKey(value);
    }
}
