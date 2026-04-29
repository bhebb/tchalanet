package com.tchalanet.server.core.pagemodel.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.pagemodel.application.command.model.CreateDraftFromTemplateUpdateCommand;
import com.tchalanet.server.core.pagemodel.application.command.model.IgnoreTemplateUpdateCommand;
import com.tchalanet.server.core.pagemodel.application.command.model.MergePageModelWithTemplateCommand;
import com.tchalanet.server.core.pagemodel.application.command.model.ReplacePageModelFromTemplateCommand;
import com.tchalanet.server.core.pagemodel.application.query.model.PreviewTemplateUpdateQuery;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/page-model-template-updates")
@RequiredArgsConstructor
@Tag(name = "Admin • PageModel Template Updates")
@PreAuthorize("hasAuthority('TENANT_ADMIN') or hasAuthority('SUPER_ADMIN')")
public class PageModelTemplateUpdateController {

  private final QueryBus queryBus;
  private final CommandBus commandBus;

  @GetMapping("/{logicalId}/preview")
  public ApiResponse<?> preview(@PathVariable String logicalId) {
    return ApiResponse.success(queryBus.send(new PreviewTemplateUpdateQuery(logicalId)));
  }

  @PostMapping("/{logicalId}/merge")
  public ApiResponse<?> merge(
      @PathVariable String logicalId, @CurrentContext TchRequestContext context) {
    return ApiResponse.success(
        commandBus.send(new MergePageModelWithTemplateCommand(logicalId, context.userId())));
  }

  @PostMapping("/{logicalId}/draft")
  public ApiResponse<?> draft(
      @PathVariable String logicalId, @CurrentContext TchRequestContext context) {
    return ApiResponse.success(
        commandBus.send(new CreateDraftFromTemplateUpdateCommand(logicalId, context.userId())));
  }

  @PostMapping("/{logicalId}/replace")
  public ApiResponse<?> replace(
      @PathVariable String logicalId, @CurrentContext TchRequestContext context) {
    return ApiResponse.success(
        commandBus.send(new ReplacePageModelFromTemplateCommand(logicalId, context.userId())));
  }

  @PostMapping("/{logicalId}/ignore")
  public ApiResponse<?> ignore(
      @PathVariable String logicalId,
      @RequestParam(required = false) NotificationId notificationId,
      @CurrentContext TchRequestContext context) {
    return ApiResponse.success(
        commandBus.send(
            new IgnoreTemplateUpdateCommand(
                logicalId, Optional.ofNullable(notificationId), context.userId())));
  }
}
