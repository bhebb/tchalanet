package com.tchalanet.server.draw.web;

import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.usecase.DrawChannelCrudUseCase;
import com.tchalanet.server.draw.web.dto.DrawChannelDto;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/draw-channels")
@RequiredArgsConstructor
public class DrawChannelController {

  private final DrawChannelCrudUseCase useCase;

  @GetMapping
  public List<DrawChannelDto> list(@RequestParam(required = false) UUID tenantId) {
    return (tenantId == null ? useCase.listAll() : useCase.listByTenant(tenantId))
        .stream().map(this::toDto).collect(Collectors.toList());
  }

  @GetMapping("/{id}")
  public ResponseEntity<DrawChannelDto> get(@PathVariable UUID id) {
    return useCase
        .get(id)
        .map(d -> ResponseEntity.ok(toDto(d)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<DrawChannelDto> create(@RequestBody DrawChannelDto dto) {
    DrawChannel d = fromDto(dto);
    var saved = useCase.create(d);
    return ResponseEntity.ok(toDto(saved));
  }

  @PutMapping("/{id}")
  public ResponseEntity<DrawChannelDto> update(
      @PathVariable UUID id, @RequestBody DrawChannelDto dto) {
    DrawChannel d = fromDto(dto);
    var updated = useCase.update(id, d);
    return ResponseEntity.ok(toDto(updated));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    useCase.delete(id);
    return ResponseEntity.noContent().build();
  }

  private DrawChannelDto toDto(DrawChannel d) {
    return DrawChannelDto.builder()
        .id(d.getId() == null ? null : d.getId().value())
        .tenantId(d.getTenantId() == null ? null : d.getTenantId().value())
        .code(d.getCode())
        .name(d.getName())
        .gameCode(d.getGameCode())
        .timezone(d.getTimezone())
        .drawTime(d.getDrawTime())
        .cutoffSec(d.getCutoffSec())
        .daysOfWeek(d.getDaysOfWeek())
        .active(d.getActive())
        .sortOrder(d.getSortOrder())
        .createdAt(d.getCreatedAt())
        .updatedAt(d.getUpdatedAt())
        .build();
  }

  private DrawChannel fromDto(DrawChannelDto dto) {
    return DrawChannel.builder()
        .id(
            dto.getId() == null
                ? null
                : new com.tchalanet.server.common.domain.DrawChannelId(dto.getId()))
        .tenantId(
            dto.getTenantId() == null
                ? null
                : new com.tchalanet.server.common.domain.TenantId(dto.getTenantId()))
        .code(dto.getCode())
        .name(dto.getName())
        .gameCode(dto.getGameCode())
        .timezone(dto.getTimezone())
        .drawTime(dto.getDrawTime())
        .cutoffSec(dto.getCutoffSec())
        .daysOfWeek(dto.getDaysOfWeek())
        .active(dto.getActive())
        .sortOrder(dto.getSortOrder())
        .build();
  }
}
