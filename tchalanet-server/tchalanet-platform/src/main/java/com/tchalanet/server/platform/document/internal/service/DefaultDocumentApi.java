package com.tchalanet.server.platform.document.internal.service;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.document.api.DocumentApi;
import com.tchalanet.server.platform.document.api.error.DocumentErrorCodes;
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
    if (request == null) {
      throw ProblemRest.of(DocumentErrorCodes.REQUEST_INVALID);
    }
    DocumentRenderer renderer = renderers.get(request.format());
    if (renderer == null) {
      throw ProblemRest.of(DocumentErrorCodes.RENDERER_UNAVAILABLE);
    }
    try {
      return renderer.render(request);
    } catch (RuntimeException ex) {
      throw ProblemRest.of(DocumentErrorCodes.RENDER_FAILED, Map.of(), ex);
    }
  }
}
