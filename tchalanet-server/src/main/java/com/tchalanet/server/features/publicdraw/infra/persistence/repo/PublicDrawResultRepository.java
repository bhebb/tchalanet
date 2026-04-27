package com.tchalanet.server.features.publicdraw.infra.persistence.repo;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.features.publicdraw.infra.persistence.PublicDrawResultRow;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface PublicDrawResultRepository {
  TchPage<PublicDrawResultRow> search(
      String slotKey, String provider, LocalDate from, LocalDate to, Pageable pageable);

  Optional<PublicDrawResultRow> findOne(String slotKey, LocalDate drawDate);

  List<PublicDrawResultRow> latest(int limitPerSlot);
}
