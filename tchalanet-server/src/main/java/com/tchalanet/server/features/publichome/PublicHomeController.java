package com.tchalanet.server.features.publichome;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicHomeController {

    private final PublicHomeService publicHomeService;

    @GetMapping("/home")
    public ResponseEntity<PublicHomeResponse> getPublicHome(@RequestParam(name = "lang", required = false) String lang) {
        return ResponseEntity.ok(publicHomeService.getPublicHome(Optional.ofNullable(lang)));
    }
}

