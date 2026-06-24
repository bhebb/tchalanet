package com.tchalanet.server.catalog.plan.internal.web;

import com.tchalanet.server.catalog.plan.api.PlanCatalog;
import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.catalog.plan.internal.write.PlanAdminService;
import com.tchalanet.server.common.types.id.PlanId;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.error.ProblemRest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/platform/plans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlanAdminController {

    private final PlanCatalog planCatalog;
    private final PlanAdminService planAdminService;

    @GetMapping
    public ApiResponse<List<PlanView>> list() {
        return ApiResponse.success(planCatalog.listActive());
    }

    @GetMapping("/{id}")
    public ApiResponse<PlanView> get(@PathVariable PlanId id) {
        return ApiResponse.success(
            planCatalog.findById(id)
                .orElseThrow(() -> ProblemRest.notFound("Plan not found", id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlanView> create(@Valid @RequestBody PlanAdminService.PlanCreateRequest request) {
        return ApiResponse.created(planAdminService.create(request));
    }

    @PatchMapping("/{id}/metadata")
    public ApiResponse<PlanView> updateMetadata(
        @PathVariable PlanId id,
        @Valid @RequestBody PlanAdminService.PlanMetadataUpdateRequest request
    ) {
        return ApiResponse.success(planAdminService.updateMetadata(id, request));
    }

    @PutMapping("/{id}/features")
    public ApiResponse<PlanView> replaceFeatures(
        @PathVariable PlanId id,
        @Valid @RequestBody PlanAdminService.PlanFeaturesUpdateRequest request
    ) {
        return ApiResponse.success(planAdminService.replaceFeatures(id, request));
    }

    @PutMapping("/{id}/limits")
    public ApiResponse<PlanView> replaceLimits(
        @PathVariable PlanId id,
        @Valid @RequestBody PlanAdminService.PlanLimitsUpdateRequest request
    ) {
        return ApiResponse.success(planAdminService.replaceLimits(id, request));
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
