package com.tchalanet.server.platform.archive.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.archive.api.ArchiveApi;
import com.tchalanet.server.platform.archive.api.model.ArchiveRunView;
import com.tchalanet.server.platform.archive.api.model.TriggerArchiveRunRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform archive management endpoints (SUPER_ADMIN only).
 *
 * <p>External route prefix: {@code /api/v1/platform/archive/**}.
 * Controller mapping: {@code /platform/archive/**} (no /api/v1).
 */
@Tag(name = "Platform - Archive")
@RestController
@RequestMapping("/platform/archive")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformArchiveController {

  private final ArchiveApi archiveApi;

  @Operation(summary = "Trigger an archive run")
  @PostMapping("/runs")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<ArchiveRunView> triggerRun(
      @Valid @RequestBody TriggerArchiveRunRequest request,
      @CurrentContext TchRequestContext ctx) {

    return ApiResponse.success(
        archiveApi.triggerRun(request, ctx.actorId().value()));
  }

  @Operation(summary = "List recent archive runs")
  @GetMapping("/runs")
  public ApiResponse<List<ArchiveRunView>> listRuns(
      @RequestParam(defaultValue = "50") int limit) {

    return ApiResponse.success(archiveApi.listRuns(limit));
  }
}
