package com.tchalanet.server.platform.document.api;

import com.tchalanet.server.platform.document.api.model.DocumentRenderRequest;
import com.tchalanet.server.platform.document.api.model.RenderedDocument;

public interface DocumentApi {

  RenderedDocument render(DocumentRenderRequest request);
}
