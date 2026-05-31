package com.tchalanet.server.catalog.i18n.internal.web;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.api.model.I18nBundleView;
import com.tchalanet.server.catalog.i18n.api.model.I18nSurface;
import com.tchalanet.server.catalog.i18n.api.model.PublicI18nSurfacePolicy;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

/**
 * Public i18n runtime read endpoint.
 *
 * <p>No authentication required. Only public surfaces are accepted.
 * Any private surface in the request causes 400 invalid_public_surface.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
@Tag(name = "Public • i18n", description = "Public runtime i18n bundle reads")
public class PublicI18nRuntimeController {

    private final I18nOverridesCatalog catalog;

    @Operation(
        summary = "Load public i18n bundle",
        description = "Returns translations grouped by surface. surface is required. Any private surface causes 400.")
    @GetMapping("/public/i18n")
    public ApiResponse<I18nBundleView> getBundle(
        @RequestParam String locale,
        @RequestParam List<I18nSurface> surface) {

        if (surface == null || surface.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "surface is required");
        }

        Set<I18nSurface> requested = Set.copyOf(surface);
        if (!PublicI18nSurfacePolicy.publicSurfaces().containsAll(requested)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_public_surface");
        }

        return ApiResponse.success(catalog.loadBundle(locale, requested));
    }
}
