package com.tchalanet.server.core.offlinesync.internal.infra.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.offlinesync.api.command.ApproveOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.api.command.RejectOfflineSubmissionCommand;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/admin/offline-sync/submissions")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
public class OfflineReviewAdminController {

  private final CommandBus commandBus;

  public OfflineReviewAdminController(CommandBus commandBus) {
    this.commandBus = commandBus;
  }

  @PostMapping("/{submissionId}/approve")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<Map<String, String>> approve(
      @CurrentContext TchRequestContext ctx,
      @PathVariable String submissionId,
      @RequestBody ReviewRequest request) {
    var result = commandBus.execute(new ApproveOfflineSubmissionCommand(
        OfflineSaleSubmissionId.parse(submissionId),
        ctx.currentUserIdRequired(),
        request.reason()));
    return ApiResponse.success(Map.of("submissionId", result.submissionId().value().toString()));
  }

  @PostMapping("/{submissionId}/reject")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<Map<String, String>> reject(
      @CurrentContext TchRequestContext ctx,
      @PathVariable String submissionId,
      @RequestBody ReviewRequest request) {
    var result = commandBus.execute(new RejectOfflineSubmissionCommand(
        OfflineSaleSubmissionId.parse(submissionId),
        ctx.currentUserIdRequired(),
        request.reason()));
    return ApiResponse.success(Map.of("submissionId", result.submissionId().value().toString(), "status", result.status()));
  }

  public record ReviewRequest(String reason) {}
}

