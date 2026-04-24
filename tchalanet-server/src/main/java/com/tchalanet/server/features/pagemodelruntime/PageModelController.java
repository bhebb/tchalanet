package com.tchalanet.server.features.pagemodelruntime;

import com.tchalanet.server.common.web.api.ApiResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/pagemodel")
@RequiredArgsConstructor
public class PageModelController {

  private final PageModelRuntimeService runtimeService;

  @GetMapping("/{logicalId}")
  public ResponseEntity<ApiResponse<PageModelRuntimeResponse>> resolve(
      @PathVariable String logicalId,
      @RequestParam(required = false) String lang) {
    var resp = runtimeService.resolvePublic(logicalId, Optional.ofNullable(lang));
    return ResponseEntity.ok(ApiResponse.success(resp));
  }
}

