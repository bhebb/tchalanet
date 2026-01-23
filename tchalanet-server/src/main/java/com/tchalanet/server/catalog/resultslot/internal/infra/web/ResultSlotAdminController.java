package com.tchalanet.server.catalog.resultslot.internal.infra.web;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.catalog.resultslot.internal.admin.ResultSlotAdminService;
import com.tchalanet.server.catalog.resultslot.internal.admin.ResultSlotAdminService.CreateResultSlotRequest;
import com.tchalanet.server.catalog.resultslot.internal.admin.ResultSlotAdminService.UpdateResultSlotRequest;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/result-slots")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Platform • Result Slots")
public class ResultSlotAdminController {

  private final ResultSlotCatalog catalog; // read-only endpoints can use the catalog
  private final ResultSlotAdminService admin; // writes

  @Operation(summary = "List active result slots (platform)")
  @GetMapping("/active")
  public ApiResponse<List<ResultSlotView>> listActive() {
    return ApiResponse.success(catalog.listActive());
  }

  @Operation(summary = "Get result slot by key (platform)")
  @GetMapping("/by-key/{slotKey}")
  public ApiResponse<ResultSlotView> getByKey(@PathVariable String slotKey) {
    return ApiResponse.success(catalog.findByKey(slotKey).orElse(null));
  }

  @Operation(summary = "Create result slot (platform)")
  @PostMapping
  public ApiResponse<ResultSlotView> create(@RequestBody CreateResultSlotRequest request) {
    var created = admin.create(request);
    return ApiResponse.created(ResultSlotView.fromEntity(created));
  }

  @Operation(summary = "Update result slot (platform)")
  @PutMapping("/{id}")
  public ApiResponse<ResultSlotView> update(@PathVariable UUID id, @RequestBody UpdateResultSlotRequest request) {
    var updated = admin.update(id, request);
    return ApiResponse.success(ResultSlotView.fromEntity(updated));
  }

  @Operation(summary = "Soft-delete result slot (platform)")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    admin.softDelete(id);
    return ApiResponse.success(null);
  }
}
