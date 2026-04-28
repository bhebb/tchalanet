package com.tchalanet.server.features.ops.infra.web;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.ops.application.OpsBatchService;
import com.tchalanet.server.features.ops.dto.JobInfoResponse;
import com.tchalanet.server.features.ops.dto.StartJobRequest;
import com.tchalanet.server.features.ops.dto.StartJobResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ops controller for batch job management.
 *
 * NOTE: /api/v1 prefix is added automatically by platform config.
 *
 * Endpoints:
 * - GET    /platform/ops/batch/jobs
 * - GET    /platform/ops/batch/jobs/{jobKey}
 * - POST   /platform/ops/batch/jobs/{jobKey}:start
 */
@RestController
@RequestMapping("/platform/ops/batch/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Ops • Batch Jobs")
public class OpsBatchJobController {

    private final OpsBatchService batchService;

    @Operation(summary = "List all registered batch jobs")
    @GetMapping
    public ApiResponse<List<JobInfoResponse>> listJobs() {
        return ApiResponse.success(batchService.listJobs());
    }

    @Operation(summary = "Get job metadata")
    @GetMapping("/{jobKey}")
    public ApiResponse<JobInfoResponse> getJob(@PathVariable String jobKey) {
        return ApiResponse.success(batchService.getJob(jobKey));
    }

    @Operation(summary = "Start a batch job")
    @PostMapping("/{jobKey}:start")
    public ApiResponse<StartJobResponse> startJob(
        @PathVariable String jobKey,
        @Valid @RequestBody StartJobRequest request
    ) {
        return ApiResponse.success(batchService.startJob(jobKey, request));
    }
}
