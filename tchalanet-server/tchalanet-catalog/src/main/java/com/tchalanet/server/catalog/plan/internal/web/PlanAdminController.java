package com.tchalanet.server.catalog.plan.internal.web;

import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.catalog.plan.internal.write.PlanAdminService;
import com.tchalanet.server.common.types.id.PlanId;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/platform/plans")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class PlanAdminController {

    private final PlanAdminService planAdminService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlanView> create(@Valid @RequestBody PlanAdminService.PlanCreateRequest request) {
        return ApiResponse.created(planAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PlanView> update(
            @PathVariable PlanId id,
            @Valid @RequestBody PlanAdminService.PlanUpdateRequest request) {
        return ApiResponse.success(planAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> softDelete(@PathVariable PlanId id) {
        planAdminService.softDelete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<Void> deactivate(@PathVariable PlanId id) {
        planAdminService.deactivate(id);
        var notice = new ApiNotice(
            "PLAN_DEACTIVATED",
            "Le plan a été désactivé avec succès.",
            "plan",
            NoticeSeverity.INFO,
            Map.of("planId", id.value())
        );
        return ApiResponse.warn(null, notice);
    }
}
