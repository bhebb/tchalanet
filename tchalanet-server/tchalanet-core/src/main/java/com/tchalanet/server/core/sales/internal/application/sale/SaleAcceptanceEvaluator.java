package com.tchalanet.server.core.sales.internal.application.sale;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.model.sale.SaleActionAvailability;
import com.tchalanet.server.core.sales.api.model.sale.SaleDecision;
import com.tchalanet.server.core.sales.internal.application.service.sell.SalePreparationOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SaleAcceptanceEvaluator {

    public static final String PREVIEW_ACCEPTED_WARNING =
        "sales.preview_accepted.warning";

    private final SalePreparationOrchestrator policyService;
    private final SaleIssueFactory issueFactory;
    private final SaleExposurePlanner exposurePlanner;

    public SaleEvaluationResult evaluatePreview(SellTicketCommand command, TchRequestContext ctx) {
        return evaluate(command, ctx, SaleEvaluationMode.PREVIEW);
    }

    public SaleEvaluationResult evaluateFinal(SellTicketCommand command, TchRequestContext ctx) {
        return evaluate(command, ctx, SaleEvaluationMode.FINAL);
    }

    private SaleEvaluationResult evaluate(
        SellTicketCommand command,
        TchRequestContext ctx,
        SaleEvaluationMode mode
    ) {
        try {
            var prepared = policyService.prepareSale(command, ctx, mode);
            exposurePlanner.groupByExposureKey(command.lines());
            var issues = issueFactory.fromNotices(prepared.notices());
            var decision = prepared.requiresApproval()
                ? SaleDecision.REQUIRES_CHANGES
                : SaleDecision.ACCEPTABLE;
            return new SaleEvaluationResult(
                mode,
                decision,
                prepared,
                issues,
                decision == SaleDecision.ACCEPTABLE
                    ? new SaleActionAvailability(true, false, false, false, false, false)
                    : SaleActionAvailability.rejected(),
                sellerInstruction(decision),
                mode == SaleEvaluationMode.PREVIEW && decision == SaleDecision.ACCEPTABLE
                    ? PREVIEW_ACCEPTED_WARNING
                    : null
            );
        } catch (ProblemRestException ex) {
            var issue = issueFactory.fromProblem(ex);
            return new SaleEvaluationResult(
                mode,
                classifyProblem(ex),
                null,
                java.util.List.of(issue),
                SaleActionAvailability.rejected(),
                issue.sellerInstruction(),
                null
            );
        }
    }

    private SaleDecision classifyProblem(ProblemRestException ex) {
        var detail = ex.getProblem().getDetail();
        if (detail != null && detail.startsWith("sales.")) {
            return SaleDecision.REQUIRES_CHANGES;
        }
        return SaleDecision.REJECTED_FINAL;
    }

    private String sellerInstruction(SaleDecision decision) {
        return switch (decision) {
            case ACCEPTABLE -> "sales.acceptable.instruction";
            case REQUIRES_CHANGES -> "sales.basket_requires_changes.instruction";
            case REJECTED_FINAL -> "sales.rejected_final.instruction";
        };
    }
}
