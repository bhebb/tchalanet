package com.tchalanet.server.api;

import com.tchalanet.server.config.context.CurrentContext;
import com.tchalanet.server.config.context.RequestContext;
import com.tchalanet.server.constants.ThemeStatus;
import com.tchalanet.server.dto.ThemeCreateDto;
import com.tchalanet.server.dto.ThemeDto;
import com.tchalanet.server.dto.ThemeUpdateDto;
import com.tchalanet.server.services.ThemeCrudService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/themes")
@RequiredArgsConstructor
public class ThemeCrudController {
  private final ThemeCrudService themeCrudService;

  @GetMapping
  @PreAuthorize("hasAuthority('TENANT_READ')")
  public List<ThemeDto> list(
      @CurrentContext RequestContext context,
      @RequestParam(defaultValue = "false") boolean includeBase,
      @RequestParam(required = false) ThemeStatus status) {
    return themeCrudService.list(context.effectiveTenant(), includeBase, status);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('TENANT_READ')")
  public ThemeDto get(@CurrentContext RequestContext context, @PathVariable UUID id) {
    return themeCrudService.get(context.effectiveTenant(), id);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('TENANT_ADMIN')")
  public ThemeDto create(
      @CurrentContext RequestContext context, @Valid @RequestBody ThemeCreateDto dto) {
    return themeCrudService.create(context.effectiveTenant(), dto);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('TENANT_ADMIN')")
  public ThemeDto update(
      @CurrentContext RequestContext context,
      @PathVariable UUID id,
      @Valid @RequestBody ThemeUpdateDto dto) {
    return themeCrudService.update(context.effectiveTenant(), id, dto);
  }

  @PostMapping("/{id}/publish")
  @PreAuthorize("hasAuthority('TENANT_ADMIN')")
  public ThemeDto publish(
      @CurrentContext RequestContext context,
      @PathVariable UUID id,
      @RequestParam Integer version) {
    return themeCrudService.publish(context.effectiveTenant(), id, version);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('TENANT_ADMIN')")
  public void archive(@CurrentContext RequestContext context, @PathVariable UUID id) {
    themeCrudService.archive(context.effectiveTenant(), id);
  }
}
