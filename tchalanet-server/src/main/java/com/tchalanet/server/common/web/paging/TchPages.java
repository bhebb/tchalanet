package com.tchalanet.server.common.web.paging;

import java.util.function.Function;
import org.springframework.data.domain.Page;

public final class TchPages {
  private TchPages() {}

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
}
