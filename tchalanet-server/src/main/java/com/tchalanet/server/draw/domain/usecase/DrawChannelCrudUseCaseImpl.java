package com.tchalanet.server.draw.domain.usecase;

import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.ports.DrawChannelRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DrawChannelCrudUseCaseImpl implements DrawChannelCrudUseCase {
  private final DrawChannelRepository repo;

  @Override
  @Transactional
  public DrawChannel create(DrawChannel d) {
    return repo.save(d);
  }

  @Override
  public Optional<DrawChannel> get(UUID id) {
    return repo.findById(new com.tchalanet.server.common.domain.DrawChannelId(id));
  }

  @Override
  public List<DrawChannel> listAll() {
    // repo findAll not implemented: fallback to empty for now
    return List.of();
  }

  @Override
  public List<DrawChannel> listByTenant(UUID tenantId) {
    return repo.findByTenant(new com.tchalanet.server.common.domain.TenantId(tenantId));
  }

  @Override
  @Transactional
  public DrawChannel update(UUID id, DrawChannel d) {
    // simple implementation: ensure exists then save
    var opt = get(id);
    if (opt.isEmpty()) throw new IllegalArgumentException("DrawChannel not found: " + id);
    DrawChannel existing = opt.get();
    existing.setName(d.getName());
    existing.setGameCode(d.getGameCode());
    existing.setTimezone(d.getTimezone());
    existing.setDrawTime(d.getDrawTime());
    existing.setCutoffSec(d.getCutoffSec());
    existing.setDaysOfWeek(d.getDaysOfWeek());
    existing.setActive(d.getActive());
    existing.setSortOrder(d.getSortOrder());
    return repo.save(existing);
  }

  @Override
  @Transactional
  public void delete(UUID id) {
    repo.deleteById(new com.tchalanet.server.common.domain.DrawChannelId(id));
  }
}
