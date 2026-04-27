package com.tchalanet.server.features.publicdraw.infra.persistence.adapter;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.features.publicdraw.application.port.out.PublicDrawResultPort;
import com.tchalanet.server.features.publicdraw.infra.persistence.PublicDrawResultRow;
import com.tchalanet.server.features.publicdraw.infra.persistence.repo.PublicDrawResultRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class PublicDrawResultRepositoryAdapter implements PublicDrawResultPort {

  private final PublicDrawResultRepository repo;

  @Override
  public Page<PublicDrawResultRow> search(
      String slotKey, String provider, LocalDate from, LocalDate to, Pageable pageable) {
    TchPage<PublicDrawResultRow> p = repo.search(slotKey, provider, from, to, pageable);
    List<PublicDrawResultRow> items = p.items();
    return new PageImpl<>(items, pageable, p.totalElements());
  }

  @Override
  public Optional<PublicDrawResultRow> findOne(String slotKey, LocalDate drawDate) {
    return repo.findOne(slotKey, drawDate);
  }

  @Override
  public List<PublicDrawResultRow> latest(int limitPerSlot) {
    return repo.latest(limitPerSlot);
  }
}
