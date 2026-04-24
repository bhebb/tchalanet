package com.tchalanet.server.features.pagemodel_backup.shared;

import com.tchalanet.server.common.web.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/public/pagemodel")
@RequiredArgsConstructor
public class PublicPageModelController {

  private final PageModelRuntimeService runtimeService;

  @GetMapping("/{logicalId}")
  public ApiResponse<PageModelResponse> resolve(
      @PathVariable String logicalId,
      @RequestParam(required = false) String lang) {

    return ApiResponse.success(
        runtimeService.resolvePublic(logicalId, Optional.ofNullable(lang))
    );
  }
}

