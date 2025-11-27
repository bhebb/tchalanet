package com.tchalanet.server.draw.application;

import com.tchalanet.server.draw.application.ports.in.DrawChannelCrudUseCase;
import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.model.DrawChannelId;
import com.tchalanet.server.draw.domain.ports.DrawChannelRepository;
import com.tchalanet.server.tenant.domain.model.TenantId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawChannelCrudUseCaseImpl implements DrawChannelCrudUseCase {

  private final DrawChannelRepository drawChannelRepository;

  @Override
  public DrawChannel create(DrawChannel d) {
    log.warn("create DrawChannel is a placeholder. Implement validation and defaults if needed.");
    return drawChannelRepository.save(d);
  }

  public Optional<DrawChannel> get(DrawChannelId id) {
    return drawChannelRepository.findById(id);
  }

  @Override
  public List<DrawChannel> listByTenant(TenantId tenantId) {
    return drawChannelRepository.findByTenant(tenantId);
  }

  @Override
  public DrawChannel update(DrawChannel d) {
    log.warn("update DrawChannel is a placeholder. Implement patch/merge logic if needed.");
    return drawChannelRepository.save(d);
  }

  @Override
  public void delete(DrawChannelId id) {
    log.warn("delete DrawChannel is a placeholder. Consider soft-delete if required.");
    drawChannelRepository.deleteById(id);
  }
}
