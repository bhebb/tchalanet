package com.tchalanet.server.features.platformadmin.overview;

import com.tchalanet.server.common.apiresponse.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/overview")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformAdminOverviewController {

    private final PlatformAdminOverviewOrchestrator orchestrator;

    @GetMapping
    public ApiResponse<PlatformAdminOverviewView> overview() {
        return ApiResponse.success(orchestrator.overview());
    }
}
