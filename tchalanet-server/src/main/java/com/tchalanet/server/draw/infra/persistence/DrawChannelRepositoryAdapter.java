package com.tchalanet.server.draw.infra.persistence;

import com.tchalanet.server.common.domain.DrawChannelId;
import com.tchalanet.server.common.domain.TenantId;
import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.ports.DrawChannelRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawChannelRepositoryAdapter implements DrawChannelRepository {

  private final DrawChannelJpaRepository repo;

  @Override
  public DrawChannel save(DrawChannel d) {
    var e = DrawChannelMapper.toEntity(d);
    var saved = repo.save(e);
    return DrawChannelMapper.toDomain(saved);
  }

  @Override
  public Optional<DrawChannel> findById(DrawChannelId id) {
    return repo.findById(id == null ? null : id.value()).map(DrawChannelMapper::toDomain);
  }

  @Override
  public List<DrawChannel> findByTenant(TenantId tenantId) {
    return repo.findByTenantIdAndActiveTrueOrderBySortOrder(tenantId.value()).stream()
        .map(DrawChannelMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<DrawChannel> findAllActive() {
    return repo.findByActiveTrueOrderBySortOrder().stream()
        .map(DrawChannelMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public void deleteById(DrawChannelId id) {
    repo.deleteById(id.value());
  }
}
