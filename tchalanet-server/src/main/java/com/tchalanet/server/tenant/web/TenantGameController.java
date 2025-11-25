package com.tchalanet.server.tenant.web;

import com.tchalanet.server.tenant.domain.model.TenantGame;
import com.tchalanet.server.tenant.domain.usecase.TenantGameCrudUseCase;
import com.tchalanet.server.tenant.web.dto.TenantGameDto;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant-games")
@RequiredArgsConstructor
public class TenantGameController {

  private final TenantGameCrudUseCase useCase;

  @GetMapping
  public List<TenantGameDto> list(@RequestParam UUID tenantId) {
    return useCase.listByTenant(tenantId).stream().map(this::toDto).collect(Collectors.toList());
  }

  @PostMapping
  public ResponseEntity<TenantGameDto> create(@RequestBody TenantGameDto dto) {
    TenantGame t = fromDto(dto);
    var saved = useCase.create(t);
    return ResponseEntity.ok(toDto(saved));
  }

  private TenantGameDto toDto(TenantGame t) {
    return TenantGameDto.builder()
        .id(t.getId() == null ? null : t.getId().value())
        .tenantId(t.getTenantId() == null ? null : t.getTenantId().value())
        .gameCode(t.getGameCode())
        .enabled(t.getEnabled())
        .displayName(t.getDisplayName())
        .minStake(t.getMinStake())
        .maxStake(t.getMaxStake())
        .flags(t.getFlags())
        .createdAt(t.getCreatedAt())
        .updatedAt(t.getUpdatedAt())
        .build();
  }

  private TenantGame fromDto(TenantGameDto d) {
    return TenantGame.builder()
        .id(
            d.getId() == null
                ? null
                : new com.tchalanet.server.common.domain.TenantGameId(d.getId()))
        .tenantId(
            d.getTenantId() == null
                ? null
                : new com.tchalanet.server.common.domain.TenantId(d.getTenantId()))
        .gameCode(d.getGameCode())
        .enabled(d.getEnabled())
        .displayName(d.getDisplayName())
        .minStake(d.getMinStake())
        .maxStake(d.getMaxStake())
        .flags(d.getFlags())
        .build();
  }
}
