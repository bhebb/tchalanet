package com.tchalanet.server.features.pagemodel.publicpage;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.pagemodel.runtime.PageRuntimeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint public (anonymous) pour résoudre l'unique page publique.
 */
@RestController
@RequestMapping("/public/page")
@RequiredArgsConstructor
@Tag(name = "Public • PageModel")
public class PublicPageModelController {

  private final PublicPageModelService service;

  @Operation(summary = "Resolve the public page")
  @GetMapping
  public ApiResponse<PageRuntimeResponse> resolve(
      @RequestParam(name = "lang", required = false) String lang) {
    return ApiResponse.success(service.resolve(Optional.ofNullable(lang)));
  }
}
