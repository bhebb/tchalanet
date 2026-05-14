package com.tchalanet.server.core.offlinesync.internal.infra.web.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReceiveOfflineBatchRequest(
    @NotBlank String grantId,
    @NotBlank String codeBatchId,
    @NotBlank String clientBatchId,
    @NotEmpty List<@Valid OfflineSubmissionRequest> submissions
) {}
