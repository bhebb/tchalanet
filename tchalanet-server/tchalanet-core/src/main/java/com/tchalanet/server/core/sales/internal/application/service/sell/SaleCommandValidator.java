package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.selection.api.SelectionApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class SaleCommandValidator {

    private final SelectionApi selectionApi;

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    public void validateCommand(SellTicketCommand command) {
        if (command.drawId() == null) {
            throw ProblemRest.badRequest("sales.draw_required");
        }
        if (command.drawChannelId() == null) {
            throw ProblemRest.badRequest("sales.draw_channel_required");
        }
        if (command.currency() == null) {
            throw ProblemRest.badRequest("sales.currency_required");
        }
        if (command.lines() == null || command.lines().isEmpty()) {
            throw ProblemRest.badRequest("sales.lines_required");
        }
        var distinctLineNumbers = command.lines().stream()
            .map(SellTicketLineInput::lineNumber)
            .distinct().count();
        if (distinctLineNumbers != command.lines().size()) {
            throw ProblemRest.badRequest("sales.duplicate_line_number");
        }
        for (var line : command.lines()) {
            validateLine(line);
        }
    }

    private void validateLine(SellTicketLineInput line) {
        if (line.lineNumber() <= 0) throw ProblemRest.badRequest("sales.invalid_line_number");
        if (line.gameCode() == null) throw ProblemRest.badRequest("sales.game_required");
        if (line.betType() == null) throw ProblemRest.badRequest("sales.bet_type_required");
        if (!line.gameCode().supports(line.betType()))
            throw ProblemRest.badRequest("sales.unsupported_bet_type");
        if (line.rawSelection() == null || line.rawSelection().isBlank())
            throw ProblemRest.badRequest("sales.selection_required");
        if (line.stakeAmount() == null || line.stakeAmount().signum() <= 0)
            throw ProblemRest.badRequest("sales.invalid_stake_amount");
        validateBetOption(line);
        validateSelection(line);
    }

    private void validateSelection(SellTicketLineInput line) {
        try {
            selectionApi.canonicalize(line.betType(), line.betOption(), line.rawSelection());
        } catch (IllegalArgumentException ex) {
            throw ProblemRest.badRequest("sales.selection_invalid");
        }
    }

    private void validateBetOption(SellTicketLineInput line) {
        try {
            BetOption.from(line.betType(), line.betOption());
        } catch (IllegalArgumentException ex) {
            if (line.betType().requiresOption() && line.betOption() == null) {
                throw ProblemRest.badRequest("sales.bet_option_required");
            }
            if (!line.betType().requiresOption() && line.betOption() != null) {
                throw ProblemRest.badRequest("sales.bet_option_not_allowed");
            }
            throw ProblemRest.badRequest("sales.bet_option_out_of_range");
        }
    }
}
