package com.tchalanet.server.features.pagemodel.publicpage;

import com.tchalanet.server.common.apiresponse.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint public (anonymous) pour résoudre un PageModel par logicalId.
 * Route : GET /api/v1/public/pagemodel/{logicalId}
 */
@RestController
@RequestMapping("/public/pagemodel")
@RequiredArgsConstructor
@Tag(name = "Public • PageModel")
public class PublicPageModelController {

  private final PublicPageModelService service;

  @Operation(summary = "Resolve public page model by logicalId")
  @GetMapping("/{logicalId}")
  public ApiResponse<PublicPageModelResponse> resolve(
      @PathVariable("logicalId") String logicalId,
      @RequestParam(name = "lang", required = false) String lang) {
    return ApiResponse.success(service.resolve(logicalId, Optional.ofNullable(lang)));
  }
}

