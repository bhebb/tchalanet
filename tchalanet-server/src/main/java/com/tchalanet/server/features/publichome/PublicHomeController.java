package com.tchalanet.server.features.publichome;

import com.tchalanet.server.common.web.api.ApiResponse;

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
public class PublicHomeController {

    private final PublicHomeService publicHomeService;

    @GetMapping("/home")
    public ResponseEntity<ApiResponse<PublicHomeResponse>> getPublicHome(
        @RequestParam(name = "lang", required = false) String lang) {
        ApiResponse<PublicHomeResponse> response =
            publicHomeService.getPublicHome(Optional.ofNullable(lang));
        return ResponseEntity.ok(response);
    }
}
