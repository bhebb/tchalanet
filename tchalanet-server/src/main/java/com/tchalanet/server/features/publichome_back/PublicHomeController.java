package com.tchalanet.server.features.publichome_back;

import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "Public • Home")
public class PublicHomeController {

  private final PublicHomeService publicHomeService;

  @Operation(summary = "Public home page content")
  @GetMapping("/home")
  public ResponseEntity<ApiResponse<PublicHomeResponse>> getPublicHome(
      @RequestParam(name = "lang", required = false) String lang) {
    var response = publicHomeService.getPublicHome(Optional.ofNullable(lang));
    return ResponseEntity.ok(response);
  }
}
