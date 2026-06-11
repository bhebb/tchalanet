package com.tchalanet.server.features.ticketverify;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.ticketverify.infra.PublicTicketRateLimiter;
import com.tchalanet.server.features.ticketverify.model.TicketVerifyRequest;
import com.tchalanet.server.features.ticketverify.model.TicketVerifyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/tickets")
@RequiredArgsConstructor
@Validated
@Tag(name = "Public • Ticket Verification")
public class TicketVerifyController {

    private final TicketVerifyService service;
    private final TicketVerifyMapper mapper;
    private final PublicTicketRateLimiter rateLimiter;


    @Operation(
        operationId = "verifyPublicTicket",
        summary = "Verify a public ticket",
        description = "Returns the public verification view for a ticket. Requires both publicCode."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid code printOptionsRequest"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PostMapping("/verify")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<TicketVerifyResponse>> verify(
        HttpServletRequest httpRequest,
        @RequestBody TicketVerifyRequest ticketVerifyRequest
    ) {
        rateLimiter.requireAllowed(httpRequest);

        var view = service.verify(ticketVerifyRequest.publicCode());
        var response = mapper.toResponse(view);

        return ResponseEntity.ok()
            .header("X-Robots-Tag", "noindex, nofollow")
            .cacheControl(CacheControl.noStore())
            .header("Pragma", "no-cache")
            .header("Expires", "0")
            .body(ApiResponse.success(response));
    }
}
