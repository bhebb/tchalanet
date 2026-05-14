package com.tchalanet.server.platform.document.api.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public record DocumentRenderRequest(
    DocumentKind kind,
    DocumentFormat format,
    String title,
    DocumentContent content,
    List<DocumentAsset> assets,
    DocumentOptions options,
    Locale locale,
    Map<String, String> metadata) {

  public DocumentRenderRequest {
    if (kind == null) throw new IllegalArgumentException("kind is required");
    if (format == null) throw new IllegalArgumentException("format is required");
    if (content == null) throw new IllegalArgumentException("content is required");
    assets = assets == null ? List.of() : List.copyOf(assets);
    options = options == null ? DocumentOptions.defaults() : options;
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }

  public DocumentAsset firstAssetOfKind(AssetKind kind) {
    for (var a : assets) {
      if (a.kind() == kind) return a;
    }
    return null;
  }
}
