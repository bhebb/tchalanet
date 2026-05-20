package com.tchalanet.server.common.web.paging;

import org.springframework.data.domain.Page;

import java.util.function.Function;

/**
 * Utility methods to convert/map between Spring Data Page and the project's TchPage, and to map
 * TchPage contents to another item type.
 */
public final class TchPageMapper {
  private TchPageMapper() {}

  public static <A, B> TchPage<B> map(Page<A> page, Function<A, B> mapper) {
    var items = page.getContent().stream().map(mapper).toList();
    return TchPage.of(
        items,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast(),
        page.hasNext(),
        page.hasPrevious());
  }

  public static <A, B> TchPage<B> map(TchPage<A> page, Function<A, B> mapper) {
    var items = page.items().stream().map(mapper).toList();
    return TchPage.of(
        items,
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages(),
        page.last(),
        page.hasNext(),
        page.hasPrevious());
  }
}
