package com.tchalanet.server.catalog.resultslot.internal.web;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.catalog.resultslot.internal.write.ResultSlotAdminService;
import com.tchalanet.server.catalog.resultslot.internal.web.model.CreateResultSlotRequest;
import com.tchalanet.server.catalog.resultslot.internal.web.model.UpdateResultSlotProjectionConfigRequest;
import com.tchalanet.server.catalog.resultslot.internal.web.model.UpdateResultSlotRequest;
import com.tchalanet.server.catalog.resultslot.internal.web.model.UpdateResultSlotSourceConfigRequest;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.types.id.ResultSlotId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/result-slots")
@RequiredArgsConstructor
@Tag(name = "Platform • Result Slots")
public class ResultSlotAdminController {

  private final ResultSlotCatalog catalog; // read-only endpoints can use the catalog
  private final ResultSlotAdminService admin; // writes

  @Operation(summary = "List active result slots (platform)")
  @GetMapping("/active")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_OWNER','TENANT_ADMIN')")
  public ApiResponse<List<ResultSlotView>> listActive() {
    return ApiResponse.success(catalog.listActive());
  }

  @Operation(summary = "Get result slot by id (platform)")
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_OWNER','TENANT_ADMIN')")
  public ApiResponse<ResultSlotView> getById(@PathVariable ResultSlotId id) {
    return ApiResponse.success(
        catalog.findById(id)
            .orElseThrow(() -> ProblemRest.notFound("Result slot not found", id)));
  }

  @Operation(summary = "Get result slot by key (platform)")
  @GetMapping("/by-key/{slotKey}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_OWNER','TENANT_ADMIN')")
  public ApiResponse<ResultSlotView> getByKey(@PathVariable String slotKey) {
    return ApiResponse.success(catalog.findByKey(slotKey).orElse(null));
  }

  @Operation(summary = "Create result slot (platform)")
  @PostMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<ResultSlotView> create(@Valid @RequestBody CreateResultSlotRequest request) {
    var createdView = admin.create(request);
    return ApiResponse.created(createdView);
  }

  @Operation(summary = "Update result slot (platform)")
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<ResultSlotView> update(@PathVariable ResultSlotId id, @Valid @RequestBody UpdateResultSlotRequest request) {
    var updatedView = admin.update(id, request);
    return ApiResponse.success(updatedView);
  }

  @Operation(summary = "Update result slot source config (expert platform setting)")
  @PutMapping("/{id}/source-config")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<ResultSlotView> updateSourceConfig(
      @PathVariable ResultSlotId id,
      @Valid @RequestBody UpdateResultSlotSourceConfigRequest request) {
    return ApiResponse.success(admin.updateSourceConfig(id, request));
  }

  @Operation(summary = "Update result slot projection config (expert platform setting)")
  @PutMapping("/{id}/projection-config")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<ResultSlotView> updateProjectionConfig(
      @PathVariable ResultSlotId id,
      @Valid @RequestBody UpdateResultSlotProjectionConfigRequest request) {
    return ApiResponse.success(admin.updateProjectionConfig(id, request));
  }

  @Operation(summary = "Soft-delete result slot (platform)")
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Void> delete(@PathVariable ResultSlotId id) {
    admin.softDelete(id);
    return ApiResponse.success(null);
  }

  @Operation(summary = "Disable result slot — kill switch (platform)")
  @PostMapping("/{slotKey}/disable")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Void> disableSlot(@PathVariable String slotKey) {
    admin.disableSlot(slotKey);
    return ApiResponse.success(null);
  }

  @Operation(summary = "Disable a game within a result slot — kill switch (platform)")
  @PostMapping("/{slotKey}/games/{gameKey}/disable")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Void> disableGame(@PathVariable String slotKey, @PathVariable String gameKey) {
    admin.disableGame(slotKey, gameKey);
    return ApiResponse.success(null);
  }
}
