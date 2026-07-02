package com.tchalanet.server.core.sales.internal.application.sale;

import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueSeverity;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueView;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SaleIssueFactory {

    public List<SaleIssueView> fromNotices(List<ApiNotice> notices) {
        if (notices == null || notices.isEmpty()) {
            return List.of();
        }
        return notices.stream()
            .map(this::fromNotice)
            .toList();
    }

    public SaleIssueView fromProblem(ProblemRestException ex) {
        var detail = ex.getProblem().getDetail();
        var code = toIssueCode(detail);
        return SaleIssueView.basket(
            code,
            SaleIssueSeverity.ERROR,
            messageCode(code),
            sellerInstruction(code),
            Map.of("problemStatus", ex.getProblem().getStatus())
        );
    }

    private SaleIssueView fromNotice(ApiNotice notice) {
        var code = toIssueCode(notice.code());
        return SaleIssueView.basket(
            code,
            toSeverity(notice.severity() == null ? null : notice.severity().name()),
            messageCode(code),
            sellerInstruction(code),
            notice.meta()
        );
    }

    private SaleIssueSeverity toSeverity(String severity) {
        if (severity == null) {
            return SaleIssueSeverity.INFO;
        }
        return switch (severity) {
            case "ERROR" -> SaleIssueSeverity.ERROR;
            case "WARN", "WARNING" -> SaleIssueSeverity.WARNING;
            default -> SaleIssueSeverity.INFO;
        };
    }

    private String toIssueCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return "SALE_EVALUATION_FAILED";
        }
        var normalized = rawCode.trim()
            .replace("sales.", "")
            .replace("notice.", "")
            .replace('.', '_')
            .replace('-', '_')
            .replace(' ', '_')
            .toUpperCase(Locale.ROOT);

        var mapped = switch (normalized) {
            case "APPROVAL_REQUIRED" -> "sales.approval_required";
            case "LIMIT_BLOCKED", "LIMITS_BLOCKED", "EXPOSURE_LIMIT", "SELECTION_EXPOSURE_LIMIT_EXCEEDED" ->
                "sales.selection_exposure_limit_exceeded";
            case "EXPOSURE_CHANGED" -> "sales.exposure_changed";
            case "DRAW_REQUIRED", "DRAW_CHANNEL_REQUIRED" -> "sales.draw_required";
            case "DRAW_CUTOFF_EXCEEDED", "DRAW_CUTOFF_TIME_HAS_PASSED" -> "sales.draw_cutoff_exceeded";
            case "DRAW_CLOSED", "DRAW_NOT_OPEN", "DRAW_IS_NOT_OPEN_FOR_SALES" -> "sales.draw_closed";
            case "CURRENCY_REQUIRED" -> "sales.currency_required";
            case "LINES_REQUIRED" -> "sales.lines_required";
            case "DUPLICATE_LINE_NUMBER" -> "sales.duplicate_line_number";
            case "GAME_REQUIRED" -> "sales.game_required";
            case "BET_TYPE_REQUIRED" -> "sales.bet_type_required";
            case "UNSUPPORTED_BET_TYPE" -> "sales.unsupported_bet_type";
            case "SELECTION_REQUIRED" -> "sales.selection_required";
            case "SELECTION_INVALID", "INVALID_LINE_NUMBER" -> "sales.invalid_selection_format";
            case "INVALID_STAKE_AMOUNT" -> "sales.invalid_stake_amount";
            case "BET_OPTION_REQUIRED" -> "sales.bet_option_required";
            case "BET_OPTION_OUT_OF_RANGE" -> "sales.bet_option_out_of_range";
            case "BET_OPTION_NOT_ALLOWED" -> "sales.bet_option_not_allowed";
            case "SESSION_CLOSED" -> "sales.session_closed";
            case "TERMINAL_BLOCKED" -> "sales.terminal_blocked";
            case "OUTLET_SUSPENDED" -> "sales.outlet_suspended";
            case "TENANT_DISABLED" -> "sales.tenant_disabled";
            case "UNTRUSTED_OPERATIONAL_CONTEXT" -> "sales.untrusted_operational_context";
            case "STAKE_TOO_HIGH" -> "sales.stake_too_high";
            case "STAKE_TOO_LOW" -> "sales.stake_too_low";
            case "BASKET_LINE_COUNT_EXCEEDED" -> "sales.basket_line_count_exceeded";
            case "BASKET_TOTAL_EXCEEDED" -> "sales.basket_total_exceeded";
            default -> null;
        };

        if (mapped != null) {
            return mapped;
        }

        log.warn("Mapping unknown sale notice code '{}' to issue code '{}'", rawCode, normalized);
        return "sales." + normalized.toLowerCase(Locale.ROOT);
    }

    private String messageCode(String code) {
        return code + ".message";
    }

    private String sellerInstruction(String code) {
        return switch (code) {
            case "sales.approval_required" -> "sales.approval_required.instruction";
            case "sales.invalid_selection_format" -> "sales.invalid_selection_format.instruction";
            case "sales.selection_exposure_limit_exceeded", "sales.exposure_changed" ->
                "sales.selection_exposure_limit_exceeded.instruction";
            case "sales.draw_cutoff_exceeded", "sales.draw_closed" -> "sales.draw_closed.instruction";
            case "sales.session_closed" -> "sales.session_closed.instruction";
            case "sales.terminal_blocked" -> "sales.terminal_blocked.instruction";
            case "sales.outlet_suspended" -> "sales.outlet_suspended.instruction";
            case "sales.tenant_disabled" -> "sales.tenant_disabled.instruction";
            default -> "sales.basket_requires_changes.instruction";
        };
    }
}
