package com.tchalanet.server.catalog.resultslot.internal.web;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCalendarCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCalendarOverrideView;
import com.tchalanet.server.catalog.resultslot.internal.web.model.CreateResultSlotCalendarOverrideRequest;
import com.tchalanet.server.catalog.resultslot.internal.web.model.UpdateResultSlotCalendarOverrideRequest;
import com.tchalanet.server.catalog.resultslot.internal.write.ResultSlotCalendarAdminService;
import com.tchalanet.server.common.types.id.ResultSlotCalendarOverrideId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** SUPER_ADMIN CRUD for the global provider calendar (no-draw days). */
@RestController
@RequestMapping("/platform/result-slots/{resultSlotId}/calendar")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Platform • Result Slot Calendar")
public class ResultSlotCalendarAdminController {

  private final ResultSlotCalendarCatalog catalog;
  private final ResultSlotCalendarAdminService admin;

  @Operation(summary = "List provider calendar overrides for a result slot")
  @GetMapping
  public ApiResponse<List<ResultSlotCalendarOverrideView>> list(
      @PathVariable ResultSlotId resultSlotId) {
    return ApiResponse.success(catalog.listBySlot(resultSlotId));
  }

  @Operation(summary = "Create a provider calendar override (specific date or recurring MM-dd)")
  @PostMapping
  public ApiResponse<ResultSlotCalendarOverrideView> create(
      @PathVariable ResultSlotId resultSlotId,
      @Valid @RequestBody CreateResultSlotCalendarOverrideRequest request) {
    return ApiResponse.created(admin.create(resultSlotId, request));
  }

  @Operation(summary = "Update a provider calendar override (availability/reason)")
  @PutMapping("/{overrideId}")
  public ApiResponse<ResultSlotCalendarOverrideView> update(
      @PathVariable ResultSlotId resultSlotId,
      @PathVariable ResultSlotCalendarOverrideId overrideId,
      @Valid @RequestBody UpdateResultSlotCalendarOverrideRequest request) {
    return ApiResponse.success(admin.update(overrideId, request));
  }

  @Operation(summary = "Soft-delete a provider calendar override")
  @DeleteMapping("/{overrideId}")
  public ApiResponse<Void> delete(
      @PathVariable ResultSlotId resultSlotId,
      @PathVariable ResultSlotCalendarOverrideId overrideId) {
    admin.softDelete(overrideId);
    return ApiResponse.success(null);
  }
}
