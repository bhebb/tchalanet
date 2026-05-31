package com.tchalanet.server.platform.keymanagement.internal.web;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.keymanagement.api.BackendPublicKeyApi;
import com.tchalanet.server.platform.keymanagement.api.model.BackendPublicKeySetView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/security/backend-signing-keys")
@RequiredArgsConstructor
@Tag(name = "Security • Backend Public Keys")
public class BackendPublicKeysController {

    private final BackendPublicKeyApi backendPublicKeyApi;

    @GetMapping
    @Operation(summary = "List active backend public signing keys (no auth required)")
    public ApiResponse<BackendPublicKeySetView> listActiveKeys() {
        return ApiResponse.success(backendPublicKeyApi.listActivePublicKeys());
    }
}
