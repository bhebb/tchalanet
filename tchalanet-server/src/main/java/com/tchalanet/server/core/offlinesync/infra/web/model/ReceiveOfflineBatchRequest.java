package com.tchalanet.server.core.offlinesync.infra.web.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReceiveOfflineBatchRequest(
    @NotBlank String terminalId,
    @NotBlank String grantId,
    @NotBlank String codeBatchId,
    @NotBlank String clientBatchId,
    @NotEmpty List<@Valid OfflineSubmissionRequest> submissions
) {}
