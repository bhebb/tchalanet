package com.tchalanet.server.core.sales.internal.application.sale;

import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueSeverity;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueView;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

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
            detail,
            sellerInstruction(code),
            Map.of("problemStatus", ex.getProblem().getStatus())
        );
    }

    private SaleIssueView fromNotice(ApiNotice notice) {
        var code = toIssueCode(notice.code());
        return SaleIssueView.basket(
            code,
            toSeverity(notice.severity() == null ? null : notice.severity().name()),
            notice.message(),
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
            .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "APPROVAL_REQUIRED" -> "APPROVAL_REQUIRED";
            case "LIMIT_BLOCKED" -> "SELECTION_EXPOSURE_LIMIT_EXCEEDED";
            case "DRAW_REQUIRED", "DRAW_CHANNEL_REQUIRED", "CURRENCY_REQUIRED", "LINES_REQUIRED",
                "INVALID_LINE_NUMBER", "GAME_REQUIRED", "BET_TYPE_REQUIRED", "UNSUPPORTED_BET_TYPE",
                "SELECTION_REQUIRED", "INVALID_STAKE_AMOUNT", "BET_OPTION_REQUIRED",
                "BET_OPTION_OUT_OF_RANGE", "BET_OPTION_NOT_ALLOWED" -> "INVALID_SELECTION_FORMAT";
            default -> normalized;
        };
    }

    private String sellerInstruction(String code) {
        return switch (code) {
            case "APPROVAL_REQUIRED" -> "Reduisez la mise, modifiez le panier ou contactez un admin.";
            case "INVALID_SELECTION_FORMAT" -> "Corrigez la selection puis reessayez.";
            case "SELECTION_EXPOSURE_LIMIT_EXCEEDED", "EXPOSURE_CHANGED" ->
                "Reduisez la mise ou retirez la ligne concernee.";
            case "DRAW_CUTOFF_EXCEEDED", "DRAW_CLOSED" -> "Choisissez un autre tirage disponible.";
            case "SESSION_CLOSED" -> "Ouvrez une session de caisse avant de vendre.";
            default -> "Modifiez le panier puis reessayez.";
        };
    }
}
