package com.tchalanet.testfixtures.security;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ArchUnit regression fixture (test-only). Deliberately lives OUTSIDE
 * {@code com.tchalanet.server} so it is never picked up by the real
 * {@code SecurityArchTest} scan, other arch rules, or Spring component scan.
 *
 * <p>It is a controller under a protected scope ({@code /admin}) whose only
 * handler returns {@code void} and carries NO {@code @PreAuthorize}/{@code @Secured}.
 * The security rule MUST flag it — this fixture pins the void-handler gap so it
 * cannot silently return.
 */
@RestController
@RequestMapping("/admin/arch-fixture")
public class ProtectedVoidUnsecuredController {

    @PostMapping("/cancel")
    public void cancel() {
        // no-op: fixture for the void-handler authorization gap
    }
}
