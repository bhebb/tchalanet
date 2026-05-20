package com.tchalanet.server.platform.document.internal.render;

import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.RenderedDocument;

public interface DocumentRenderer {
  DocumentFormat format();

  RenderedDocument render(DocumentRenderRequest request);
}
