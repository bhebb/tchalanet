package com.tchalanet.server.features.pagemodel.publicpage;

import com.tchalanet.server.common.web.api.ApiResponse;
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
 * Endpoint public (anonymous) pour résoudre un PageModel public par logicalId.
 * Route : GET /api/v1/public/page-models/{logicalId}
 * Le logicalId est explicite pour les pages publiques (ex: public.home, public.draw_results).
 */
@RestController
@RequestMapping("/public/page-models")
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

