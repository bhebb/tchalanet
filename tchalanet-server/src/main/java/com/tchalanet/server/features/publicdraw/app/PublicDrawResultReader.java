package com.tchalanet.server.features.publicdraw.app;

import com.tchalanet.server.features.publicdraw.persistence.PublicDrawResultRow;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PublicDrawResultReader {
  Page<PublicDrawResultRow> search(
      String slotKey, String provider, LocalDate from, LocalDate to, Pageable pageable);

  Optional<PublicDrawResultRow> findOne(String slotKey, LocalDate drawDate);

  List<PublicDrawResultRow> latest(int limitPerSlot);
}
