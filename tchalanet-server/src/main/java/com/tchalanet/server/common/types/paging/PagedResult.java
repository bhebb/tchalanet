package com.tchalanet.server.common.types.paging;

import java.util.List;

/** Value object for paged results. */
public record PagedResult<T>(List<T> content, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {

  public PagedResult {
    if (content == null) throw new IllegalArgumentException("PagedResult.content is null");
    if (totalElements < 0) throw new IllegalArgumentException("PagedResult.totalElements must be non-negative");
    if (totalPages < 0) throw new IllegalArgumentException("PagedResult.totalPages must be non-negative");
  }

  /**
   * Static factory for PagedResult.
   */
  public static <T> PagedResult<T> of(List<T> content, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {
    return new PagedResult<>(content, totalElements, totalPages, hasNext, hasPrevious);
  }
}
