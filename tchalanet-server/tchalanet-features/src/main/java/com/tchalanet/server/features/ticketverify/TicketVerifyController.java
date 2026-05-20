package com.tchalanet.server.features.ticketverify;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.ticketverify.infra.PublicTicketRateLimiter;
import com.tchalanet.server.features.ticketverify.model.TicketVerifyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        description = "Returns the public verification view for a ticket. Requires both publicCode and verificationCode."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid code format"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping("/{publicCode}/verify")
    public ResponseEntity<ApiResponse<TicketVerifyResponse>> verify(
        HttpServletRequest httpRequest,
        @PathVariable
        @NotBlank
        @Size(min = 6, max = 32)
        @Pattern(regexp = "^[A-Z0-9-]+$", message = "Invalid public code format")
        String publicCode,
        @RequestParam("verificationCode")
        @NotBlank
        @Size(min = 4, max = 32)
        String verificationCode
    ) {
        rateLimiter.requireAllowed(httpRequest);

        var view = service.verify(publicCode, verificationCode);
        var response = mapper.toResponse(view);

        return ResponseEntity.ok()
            .header("X-Robots-Tag", "noindex, nofollow")
            .header("Cache-Control", "no-store")
            .body(ApiResponse.success(response));
    }
}
