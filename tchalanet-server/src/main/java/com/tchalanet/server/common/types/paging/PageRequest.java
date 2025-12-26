package com.tchalanet.server.common.types.paging;

/** Value object for page requests. */
public record PageRequest(int page, int size) {

  public PageRequest {
    if (page < 0) throw new IllegalArgumentException("PageRequest.page must be non-negative");
    if (size <= 0) throw new IllegalArgumentException("PageRequest.size must be positive");
  }

  /** Static factory for PageRequest. */
  public static PageRequest of(int page, int size) {
    return new PageRequest(page, size);
  }
}
