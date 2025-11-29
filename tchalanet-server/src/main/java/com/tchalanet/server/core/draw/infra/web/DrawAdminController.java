package com.tchalanet.server.core.draw.infra.web;

import com.tchalanet.server.core.draw.application.command.handler.CreateDrawCommandHandler;
import com.tchalanet.server.core.draw.application.command.handler.OverrideDrawResultCommandHandler;
import com.tchalanet.server.core.draw.application.command.handler.UpdateDrawCommandHandler;
import com.tchalanet.server.core.draw.application.command.model.OverrideDrawResultCommand;
import com.tchalanet.server.core.draw.application.query.handler.ListDrawsHandler;
import com.tchalanet.server.core.draw.application.query.model.ListDrawsQuery;
import com.tchalanet.server.core.draw.infra.web.mapper.DrawAdminWebMapper;
import com.tchalanet.server.core.draw.infra.web.model.CreateDrawRequest;
import com.tchalanet.server.core.draw.infra.web.model.DrawSummaryResponse;
import com.tchalanet.server.core.draw.infra.web.model.OverrideDrawResultRequest;
import com.tchalanet.server.core.draw.infra.web.model.UpdateDrawRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST API d'administration pour la gestion des tirages. TODO: Migration hexagonale complète -
 * implémentation temporaire.
 */
@RestController
@RequestMapping("/api/v1/admin/draws")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class DrawAdminController {

  private final CreateDrawCommandHandler createDrawCommandHandler;
  private final UpdateDrawCommandHandler updateDrawCommandHandler;
  private final OverrideDrawResultCommandHandler overrideDrawResultCommandHandler;
  private final ListDrawsHandler listDrawsHandler;
  private final DrawAdminWebMapper mapper;

  @GetMapping
  public ResponseEntity<List<DrawSummaryResponse>> listDraws(@RequestParam UUID tenantId) {
    var summaries = listDrawsHandler.handle(new ListDrawsQuery(tenantId, null, null, null));
    var responses = summaries.stream().map(mapper::toDrawSummaryResponse).toList();
    return ResponseEntity.ok(responses);
  }

  @PostMapping
  public ResponseEntity<DrawSummaryResponse> createDraw(@RequestBody CreateDrawRequest request) {
    var command = mapper.toCreateDrawCommand(request);
    var drawId = createDrawCommandHandler.handle(command);
    // TODO: Fetch the created draw summary, for now return dummy
    return ResponseEntity.ok(
        new DrawSummaryResponse("code", "name", "status", null, null, false, true, List.of()));
  }

  @PutMapping("/{drawId}")
  public ResponseEntity<DrawSummaryResponse> updateDraw(
      @PathVariable UUID drawId,
      @RequestParam UUID tenantId,
      @RequestBody UpdateDrawRequest request,
      Authentication authentication) {
    // Ensure the path drawId matches the request drawId
    if (!drawId.equals(request.drawId())) {
      return ResponseEntity.badRequest().build();
    }
    // Ensure tenantId matches
    if (!tenantId.equals(request.tenantId())) {
      return ResponseEntity.badRequest().build();
    }
    var command = mapper.toUpdateDrawCommand(request);
    updateDrawCommandHandler.handle(command);
    // TODO: Fetch the updated draw summary
    return ResponseEntity.ok(
        new DrawSummaryResponse("code", "name", "status", null, null, false, true, List.of()));
  }

  @PostMapping("/{drawId}/override-result")
  public ResponseEntity<Void> overrideResult(
      @PathVariable UUID drawId,
      @RequestParam UUID tenantId,
      @RequestBody OverrideDrawResultRequest request,
      Authentication authentication) {
    UUID adminId = extractUserId(authentication);
    var command =
        new OverrideDrawResultCommand(
            drawId,
            tenantId,
            adminId,
            Instant.now(),
            request.numbers(),
            null, // numbersExtra
            request.reason() // reason
            );
    overrideDrawResultCommandHandler.handle(command);
    return ResponseEntity.ok().build();
  }

  private UUID extractUserId(Authentication authentication) {
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      try {
        // support tokens where sub is numeric or not UUID: try claim 'sub' else
        // 'preferred_username'
        Object subClaim = jwt.getClaims().get("sub");
        if (subClaim instanceof String s) {
          try {
            return UUID.fromString(s);
          } catch (Exception ex) {
            // ignore
          }
        }
        Object usernameClaim = jwt.getClaims().get("preferred_username");
        if (usernameClaim instanceof String s) {
          try {
            return UUID.fromString(s);
          } catch (Exception ex) {
            // ignore
          }
        }
      } catch (Exception e) {
        // fallthrough
      }
    }
    // fallback: unknown user
    return UUID.fromString("00000000-0000-0000-0000-000000000000");
  }
}
