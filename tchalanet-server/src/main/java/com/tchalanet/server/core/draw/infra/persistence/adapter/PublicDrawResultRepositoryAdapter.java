package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.core.draw.application.port.out.PublicDrawResultPort;
import com.tchalanet.server.core.draw.infra.persistence.PublicDrawResultRow;
import com.tchalanet.server.core.draw.infra.persistence.repo.PublicDrawResultRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicDrawResultRepositoryAdapter implements PublicDrawResultPort {

  private final PublicDrawResultRepository repository;

  @Override
  public Page<PublicDrawResultRow> search(
      String channelCode, LocalDate from, LocalDate to, Pageable pageable) {
    return repository.search(channelCode, from, to, pageable);
  }

  @Override
  public Optional<PublicDrawResultRow> findOne(String channelCode, LocalDate drawDate) {
    return repository.findOne(channelCode, drawDate);
  }

  @Override
  public List<PublicDrawResultRow> latest(int limitPerChannel) {
    return repository.latest(limitPerChannel);
  }
}
