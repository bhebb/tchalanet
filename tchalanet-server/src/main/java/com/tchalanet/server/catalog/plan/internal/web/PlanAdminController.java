package com.tchalanet.server.catalog.plan.internal.web;

import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.catalog.plan.internal.write.PlanAdminService;
import com.tchalanet.server.common.types.id.PlanId;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for platform plan admin (catalog/plan).
 * Conforme à 75-catalog-rules.md et REFACTORING_GUIDE.md.
 */
@RestController
@RequestMapping("/platform/plans")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class PlanAdminController {

    private final PlanAdminService planAdminService;

    @PostMapping
    public ResponseEntity<ApiResponse<PlanView>> create(@RequestBody PlanAdminService.PlanCreateRequest request) {
        PlanView view = planAdminService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(view));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanView>> update(@PathVariable("id") UUID id, @RequestBody PlanAdminService.PlanUpdateRequest request) {
        PlanView view = planAdminService.update(PlanId.of(id), request);
        return ResponseEntity.ok(ApiResponse.success(view));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable("id") UUID id) {
        planAdminService.softDelete(PlanId.of(id));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable("id") UUID id) {
        planAdminService.deactivate(PlanId.of(id));
        // Exemple d'ajout de notice (optionnel)
        ApiNotice notice = new ApiNotice(
            "PLAN_DEACTIVATED",
            "Le plan a été désactivé avec succès.",
            "plan",
            NoticeSeverity.INFO,
            Map.of("planId", id)
        );
        return ResponseEntity.ok(ApiResponse.warn(null, notice));
    }

    // Gestion des erreurs (exemple)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Validation error");
        pd.setType(URI.create("/problem/validation-error"));
        return ResponseEntity.badRequest().body(pd);
    }
}
