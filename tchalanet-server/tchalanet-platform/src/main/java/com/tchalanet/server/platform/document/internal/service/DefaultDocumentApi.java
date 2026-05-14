package com.tchalanet.server.platform.document.internal.service;

import com.tchalanet.server.platform.document.api.DocumentApi;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.RenderedDocument;
import com.tchalanet.server.platform.document.internal.render.DocumentRenderer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DefaultDocumentApi implements DocumentApi {

  private final Map<DocumentFormat, DocumentRenderer> renderers;

  public DefaultDocumentApi(List<DocumentRenderer> renderers) {
    Map<DocumentFormat, DocumentRenderer> map = new EnumMap<>(DocumentFormat.class);
    for (DocumentRenderer r : renderers) {
      if (map.put(r.format(), r) != null) {
        throw new IllegalStateException("Duplicate renderer for format " + r.format());
      }
    }
    this.renderers = map;
  }

  @Override
  public RenderedDocument render(DocumentRenderRequest request) {
    if (request == null) throw new IllegalArgumentException("request is required");
    DocumentRenderer renderer = renderers.get(request.format());
    if (renderer == null) {
      throw new IllegalStateException("No renderer registered for format " + request.format());
    }
    return renderer.render(request);
  }
}
