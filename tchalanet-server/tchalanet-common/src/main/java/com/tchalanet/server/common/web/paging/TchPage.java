package com.tchalanet.server.common.web.paging;

import java.util.List;

public record TchPage<T>(
    List<T> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last,
    boolean hasNext,
    boolean hasPrevious) {
  public static <T> TchPage<T> of(
      List<T> items,
      int page,
      int size,
      long totalElements,
      int totalPages,
      boolean last,
      boolean hasNext,
      boolean hasPrevious) {
    return new TchPage<>(items, page, size, totalElements, totalPages, last, hasNext, hasPrevious);
  }
}
