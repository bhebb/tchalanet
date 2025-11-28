package com.tchalanet.server.draw.domain.usecase;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.model.DrawChannelId;
import com.tchalanet.server.draw.domain.ports.DrawChannelRepository;
import com.tchalanet.server.tenant.domain.model.TenantId;
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
    return repo.findById(new DrawChannelId(id));
  }

  @Override
  public List<DrawChannel> listAll() {
    return List.of();
  }

  @Override
  public List<DrawChannel> listByTenant(UUID tenantId) {
    return repo.findByTenant(new TenantId(tenantId));
  }

  @Override
  @Transactional
  public DrawChannel update(UUID id, DrawChannel d) {
    var opt = get(id);
    if (opt.isEmpty()) throw new IllegalArgumentException("DrawChannel not found: " + id);
    DrawChannel existing = opt.get();
    // create a new instance with updated fields
    DrawChannel updated =
        new DrawChannel(
            existing.getId(),
            d.getName() != null ? d.getName() : existing.getName(),
            existing.getTenantId(),
            d.getGameCode() != null ? d.getGameCode() : existing.getGameCode(),
            d.getTimezone() != null ? d.getTimezone() : existing.getTimezone(),
            d.getDrawTime() != null ? d.getDrawTime() : existing.getDrawTime(),
            d.getCutoffSec() != null ? d.getCutoffSec() : existing.getCutoffSec(),
            d.getDaysOfWeek() != null ? d.getDaysOfWeek() : existing.getDaysOfWeek(),
            d.getActive() != null ? d.getActive() : existing.getActive(),
            d.getSortOrder() != null ? d.getSortOrder() : existing.getSortOrder());
    return repo.save(updated);
  }

  @Override
  @Transactional
  public void delete(UUID id) {
    repo.deleteById(new DrawChannelId(id));
  }
}
