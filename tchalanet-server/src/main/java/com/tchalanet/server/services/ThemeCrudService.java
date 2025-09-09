package com.tchalanet.server.services;

import com.tchalanet.server.constants.ThemeMode;
import com.tchalanet.server.constants.ThemeStatus;
import com.tchalanet.server.dto.ThemeCreateDto;
import com.tchalanet.server.dto.ThemeDto;
import com.tchalanet.server.dto.ThemeUpdateDto;
import com.tchalanet.server.model.Theme;
import com.tchalanet.server.repository.ThemeRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThemeCrudService {
  private final ThemeRepository repo;
  private final ThemeQueryService queryService;

  public List<ThemeDto> list(UUID tenantId, boolean includeBase, @Nullable ThemeStatus status) {
    var res = new ArrayList<Theme>();
    if (includeBase) res.addAll(repo.findByTenantIdIsNull());
    res.addAll(repo.findByTenantId(tenantId));
    if (status != null) res.removeIf(t -> t.getStatus() != status);
    return res.stream().map(this::toDTO).toList();
  }

  public ThemeDto get(UUID tenantId, UUID id) {
    var t = repo.findById(id).orElseThrow();
    if (t.getTenantId() != null && !t.getTenantId().equals(tenantId))
      throw new AccessDeniedException("Forbidden");
    return toDTO(t);
  }

  public ThemeDto create(UUID tenantId, ThemeCreateDto dto) {
    var t = new Theme();
    t.setId(UUID.randomUUID());
    t.setTenantId(tenantId);
    t.setBasePresetId(dto.basePresetId());
    t.setLabel(dto.label());
    t.setMode(Optional.ofNullable(dto.mode()).orElse(ThemeMode.SYSTEM));
    t.setDensity(Optional.ofNullable(dto.density()).orElse((short) 0));
    t.setPalette(Optional.ofNullable(dto.palette()).orElseGet(HashMap::new));
    t.setTokens(Optional.ofNullable(dto.tokens()).orElseGet(HashMap::new));
    t.setCssVars(Optional.ofNullable(dto.cssVars()).orElseGet(HashMap::new));
    t.setStatus(ThemeStatus.DRAFT);
    t.setVersion(1);
    var saved = repo.save(t);
    return toDTO(saved);
  }

  public ThemeDto update(UUID tenantId, UUID id, ThemeUpdateDto dto) {
    var t = repo.findById(id).orElseThrow();
    if (t.getTenantId() == null || !t.getTenantId().equals(tenantId))
      throw new AccessDeniedException("Forbidden");
    if (!Objects.equals(t.getVersion(), dto.version()))
      throw new OptimisticLockingFailureException("Version mismatch");
    if (dto.label() != null) t.setLabel(dto.label());
    if (dto.mode() != null) t.setMode(dto.mode());
    if (dto.density() != null) t.setDensity(dto.density());
    if (dto.palette() != null) t.getPalette().putAll(dto.palette());
    if (dto.tokens() != null) t.getTokens().putAll(dto.tokens());
    if (dto.cssVars() != null) t.getCssVars().putAll(dto.cssVars());
    t.setVersion(t.getVersion() + 1);
    var saved = repo.save(t);
    return toDTO(saved);
  }

  public ThemeDto publish(UUID tenantId, UUID id, Integer expectedVersion) {
    var t = repo.findById(id).orElseThrow();
    if (t.getTenantId() == null || !t.getTenantId().equals(tenantId))
      throw new AccessDeniedException("Forbidden");
    if (!Objects.equals(t.getVersion(), expectedVersion))
      throw new OptimisticLockingFailureException("Version mismatch");
    t.setStatus(ThemeStatus.PUBLISHED);
    t.setVersion(t.getVersion() + 1);
    var saved = repo.save(t);
    queryService.evictTenantTheme(tenantId); // purge cache
    return toDTO(saved);
  }

  public void archive(UUID tenantId, UUID id) {
    var t = repo.findById(id).orElseThrow();
    if (t.getTenantId() == null || !t.getTenantId().equals(tenantId))
      throw new AccessDeniedException("Forbidden");
    t.setStatus(ThemeStatus.ARCHIVED);
    t.setVersion(t.getVersion() + 1);
    repo.save(t);
    queryService.evictTenantTheme(tenantId); // purge cache
  }

  private ThemeDto toDTO(Theme t) {
    return new ThemeDto(
        t.getId(),
        t.getTenantId(),
        t.getBasePresetId(),
        t.getLabel(),
        t.getMode(),
        t.getDensity(),
        t.getPalette(),
        t.getTokens(),
        t.getCssVars(),
        t.getStatus(),
        t.getVersion(),
        t.getCreatedAt(),
        t.getUpdatedAt());
  }
}
