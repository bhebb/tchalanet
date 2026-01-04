package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.core.draw.infra.persistence.PublicDrawResultRow;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PublicDrawResultPort {
  Page<PublicDrawResultRow> search(String channelCode, LocalDate from, LocalDate to, Pageable pageable);

  Optional<PublicDrawResultRow> findOne(String channelCode, LocalDate drawDate);

  List<PublicDrawResultRow> latest(int limitPerChannel);
}
