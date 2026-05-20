package com.tchalanet.server.platform.document.api.model;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record DocumentRenderRequest(
    DocumentTemplateKey templateKey,
    DocumentKind kind,
    DocumentFormat format,
    String title,
    DocumentContent content,
    List<DocumentAsset> assets,
    DocumentOptions options,
    Locale locale,
    ZoneId timezone,
    Map<String, String> metadata
) {

    public DocumentRenderRequest {
        if (templateKey == null) {
            throw new IllegalArgumentException("templateKey is required");
        }
        Objects.requireNonNull(kind, "kind is required");
        Objects.requireNonNull(format, "format is required");
        Objects.requireNonNull(content, "content is required");

        assets = assets == null ? List.of() : List.copyOf(assets);
        options = options == null ? DocumentOptions.defaults() : options;
        locale = locale == null ? Locale.getDefault() : locale;
        timezone = timezone == null ? ZoneId.of("UTC") : timezone;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);

        if (!kind.supports(content)) {
            throw new IllegalArgumentException(
                "Document kind " + kind + " does not support content "
                    + content.getClass().getSimpleName()
            );
        }
    }


    public DocumentAsset firstAssetOfKind(AssetKind kind) {
        if (kind == null) return null;

        for (var a : assets) {
            if (a.kind() == kind) return a;
        }

        return null;
    }

    public String metadataValue(String key, String fallback) {
        var value = metadata.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }


}
