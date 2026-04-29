package com.tchalanet.server.features.publicdraw.persistence;

import com.tchalanet.server.features.publicdraw.app.PublicDrawResultReader;
import com.tchalanet.server.features.publicdraw.persistence.PublicDrawResultRow;
import com.tchalanet.server.features.publicdraw.persistence.PublicDrawResultJdbcRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicDrawResultJdbcReader implements PublicDrawResultReader {

  private final PublicDrawResultJdbcRepository jdbc;

  @Override
  public Page<PublicDrawResultRow> search(
      String slotKey, String provider, LocalDate from, LocalDate to, Pageable pageable) {

    var p = pageable == null ? Pageable.ofSize(20) : pageable;
    int size = Math.max(1, Math.min(100, p.getPageSize()));
    int page = Math.max(0, p.getPageNumber());
    int offset = page * size;

    long total = jdbc.count(slotKey, provider, from, to);
    var rows = jdbc.search(slotKey, provider, from, to, size, offset, p.getSort());

    return new PageImpl<>(rows, p, total);
  }

  @Override
  public Optional<PublicDrawResultRow> findOne(String slotKey, LocalDate drawDate) {
    return jdbc.findOne(slotKey, drawDate);
  }

  @Override
  public List<PublicDrawResultRow> latest(int limitPerSlot) {
    return jdbc.latest(limitPerSlot);
  }
}
