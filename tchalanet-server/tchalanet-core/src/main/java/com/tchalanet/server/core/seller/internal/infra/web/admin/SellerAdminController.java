package com.tchalanet.server.core.seller.internal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.seller.api.command.AssignSellerToOutletCommand;
import com.tchalanet.server.core.seller.api.command.CreateSellerCommand;
import com.tchalanet.server.core.seller.api.command.EndSellerAssignmentCommand;
import com.tchalanet.server.core.seller.api.command.LinkSellerToUserCommand;
import com.tchalanet.server.core.seller.api.command.SetSellerCommissionPolicyCommand;
import com.tchalanet.server.core.seller.api.command.UpdateSellerStatusCommand;
import com.tchalanet.server.core.seller.api.model.SellerCommissionBase;
import com.tchalanet.server.core.seller.api.model.SellerCommissionType;
import com.tchalanet.server.core.seller.api.model.SellerStatus;
import com.tchalanet.server.core.seller.api.model.SellerView;
import com.tchalanet.server.core.seller.api.query.GetSellerQuery;
import com.tchalanet.server.core.seller.api.query.ListSellersQuery;
import com.tchalanet.server.core.seller.api.query.model.SellerSummaryView;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/admin/sellers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@Tag(name = "Sellers • Admin")
public class SellerAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public record CreateSellerRequest(@NotBlank String displayName, String code, UserId userId) {}
    public record LinkUserRequest(@NotNull UserId userId) {}
    public record UpdateStatusRequest(@NotNull SellerStatus status) {}
    public record AssignOutletRequest(@NotNull OutletId outletId, @NotNull Instant startsAt) {}
    public record SetCommissionPolicyRequest(
        @NotNull SellerCommissionType type,
        @NotNull SellerCommissionBase base,
        BigDecimal ratePercent,
        BigDecimal fixedAmount,
        String currency,
        @NotNull Instant startsAt
    ) {}

    @PostMapping
    public ApiResponse<SellerId> create(@CurrentContext TchRequestContext ctx, @Valid @RequestBody CreateSellerRequest req) {
        return ApiResponse.success(commandBus.execute(new CreateSellerCommand(
            ctx.effectiveTenantIdRequired(), req.displayName(), req.code(), req.userId())));
    }

    @GetMapping("/{sellerId}")
    public ApiResponse<SellerView> get(@CurrentContext TchRequestContext ctx, @PathVariable SellerId sellerId) {
        return ApiResponse.success(queryBus.ask(new GetSellerQuery(ctx.effectiveTenantIdRequired(), sellerId)));
    }

    @GetMapping
    public ApiResponse<List<SellerSummaryView>> list(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(queryBus.ask(new ListSellersQuery(ctx.effectiveTenantIdRequired())));
    }

    @PostMapping("/{sellerId}/user")
    public ApiResponse<Void> linkUser(@CurrentContext TchRequestContext ctx, @PathVariable SellerId sellerId, @Valid @RequestBody LinkUserRequest req) {
        commandBus.execute(new LinkSellerToUserCommand(ctx.effectiveTenantIdRequired(), sellerId, req.userId()));
        return ApiResponse.success(null);
    }

    @PatchMapping("/{sellerId}/status")
    public ApiResponse<Void> updateStatus(@CurrentContext TchRequestContext ctx, @PathVariable SellerId sellerId, @Valid @RequestBody UpdateStatusRequest req) {
        commandBus.execute(new UpdateSellerStatusCommand(ctx.effectiveTenantIdRequired(), sellerId, req.status()));
        return ApiResponse.success(null);
    }

    @PostMapping("/{sellerId}/assignments")
    public ApiResponse<SellerOutletAssignmentId> assignOutlet(@CurrentContext TchRequestContext ctx, @PathVariable SellerId sellerId, @Valid @RequestBody AssignOutletRequest req) {
        return ApiResponse.success(commandBus.execute(new AssignSellerToOutletCommand(
            ctx.effectiveTenantIdRequired(), sellerId, req.outletId(), req.startsAt())));
    }

    @PostMapping("/{sellerId}/assignments/{assignmentId}/end")
    public ApiResponse<Void> endAssignment(@CurrentContext TchRequestContext ctx, @PathVariable SellerId sellerId, @PathVariable SellerOutletAssignmentId assignmentId) {
        commandBus.execute(new EndSellerAssignmentCommand(ctx.effectiveTenantIdRequired(), sellerId, assignmentId));
        return ApiResponse.success(null);
    }

    @PostMapping("/{sellerId}/commission-policy")
    public ApiResponse<Void> setCommissionPolicy(@CurrentContext TchRequestContext ctx, @PathVariable SellerId sellerId, @Valid @RequestBody SetCommissionPolicyRequest req) {
        commandBus.execute(new SetSellerCommissionPolicyCommand(
            ctx.effectiveTenantIdRequired(), sellerId, req.type(), req.base(),
            req.ratePercent(), req.fixedAmount(), req.currency(), req.startsAt()));
        return ApiResponse.success(null);
    }
}
