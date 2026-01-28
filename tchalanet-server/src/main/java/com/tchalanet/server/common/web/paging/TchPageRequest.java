package com.tchalanet.server.common.web.paging;

import org.springframework.data.domain.Pageable;

public record TchPageRequest(Pageable pageable) {
  public TchPageRequest {
    if (pageable == null) throw new IllegalArgumentException("pageable is required");
  }
}
