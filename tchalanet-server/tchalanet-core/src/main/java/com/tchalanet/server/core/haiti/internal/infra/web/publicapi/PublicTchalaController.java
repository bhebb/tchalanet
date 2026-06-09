package com.tchalanet.server.core.haiti.internal.infra.web.publicapi;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.haiti.api.command.SubmitTchalaSuggestionCommand;
import com.tchalanet.server.core.haiti.api.command.SubmitTchalaSuggestionResult;
import com.tchalanet.server.core.haiti.api.query.GetTchalaByDreamQuery;
import com.tchalanet.server.core.haiti.api.query.GetTchalaByNumberQuery;
import com.tchalanet.server.core.haiti.api.query.GetTchalaEntryQuery;
import com.tchalanet.server.core.haiti.api.query.SearchTchalaQuery;
import com.tchalanet.server.core.haiti.internal.application.command.handler.SubmitTchalaSuggestionCommandHandler;
import com.tchalanet.server.core.haiti.internal.application.port.out.TchalaEntryRepositoryPort;
import com.tchalanet.server.core.haiti.internal.domain.tchala.exception.TchalaEntryNotFoundException;
import com.tchalanet.server.core.haiti.internal.domain.tchala.model.TchalaEntry;
import com.tchalanet.server.core.haiti.internal.infra.web.model.SubmitSuggestionRequest;
import com.tchalanet.server.core.haiti.internal.infra.web.model.SubmitSuggestionResponse;
import com.tchalanet.server.core.haiti.internal.infra.web.model.TchalaEntryResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** Public REST API for Tchala catalogue and suggestions. */
@Validated
@RestController
@RequestMapping("/public/tchala")
@Tag(name = "Public • Tchala")
@RequiredArgsConstructor
public class PublicTchalaController {

  private final QueryBus queryBus;
  private final CommandBus commandBus;
  private final TchalaEntryRepositoryPort repo;

  /**
   * Get a tchala entry by id (public read). Accepts TchalaEntryId bound via the
   * String->TchalaEntryId converter.
   */
  @GetMapping("/{id}")
  public ApiResponse<TchalaEntryResponse> getById(@PathVariable @NotNull TchalaEntryId id) {
    var q = new GetTchalaEntryQuery(id);
    Optional<TchalaEntry> found = queryBus.ask(q);
    return found
        .map(e -> ApiResponse.success(TchalaEntryResponse.from(e)))
        .orElseThrow(() -> new TchalaEntryNotFoundException("Tchala entry not found"));
  }

  /** Search the catalogue for a text. Returns approved entries. */
  @GetMapping("/search")
  public ApiResponse<TchPage<TchalaEntryResponse>> search(
      @RequestParam(defaultValue = "fr") String lang,
      @RequestParam(name = "q", required = false) String text,
      @RequestParam(defaultValue = "0") @Min(0) int offset,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {

    int size = limit;
    int page = Math.max(0, offset / size);

    TchPage<TchalaEntry> res = queryBus.ask(new SearchTchalaQuery(lang, text, page, size));

    var data =
        TchPage.of(
            res.items().stream().map(TchalaEntryResponse::from).toList(),
            res.page(),
            res.size(),
            res.totalElements(),
            res.totalPages(),
            res.last(),
            res.hasNext(),
            res.hasPrevious());

    return ApiResponse.success(data);
  }

  /** Find a tchala entry by its dream normalized slotKey. */
  @GetMapping("/by-dream")
  public ApiResponse<TchalaEntryResponse> byDream(
      @RequestParam(defaultValue = "fr") String lang, @RequestParam @NotBlank String dream) {

    var q = new GetTchalaByDreamQuery(lang, dream);
    Optional<TchalaEntry> found = queryBus.ask(q);
    return found
        .map(e -> ApiResponse.success(TchalaEntryResponse.from(e)))
        .orElseThrow(() -> new TchalaEntryNotFoundException("Tchala entry not found"));
  }

  /** Find tchala entries that contain a given number. */
  @GetMapping("/by-number")
  public ApiResponse<TchPage<TchalaEntryResponse>> byNumber(
      @RequestParam(defaultValue = "fr") String lang,
      @RequestParam @Min(0) @Max(99) int number,
      @RequestParam(defaultValue = "0") @Min(0) int offset,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {

    int size = limit;
    int page = Math.max(0, offset / size);

    TchPage<TchalaEntry> res = queryBus.ask(new GetTchalaByNumberQuery(lang, number, page, size));

    var data =
        TchPage.of(
            res.items().stream().map(TchalaEntryResponse::from).toList(),
            res.page(),
            res.size(),
            res.totalElements(),
            res.totalPages(),
            res.last(),
            res.hasNext(),
            res.hasPrevious());

    return ApiResponse.success(data);
  }

  /** Returns whether the public suggestion box is open (pending count below the limit). */
  @GetMapping("/suggestions/status")
  public ApiResponse<SuggestionStatusResponse> suggestionStatus() {
    long pending = repo.countAllPending();
    int max = SubmitTchalaSuggestionCommandHandler.MAX_PENDING_SUGGESTIONS;
    boolean open = pending < max;
    return ApiResponse.success(new SuggestionStatusResponse(open, (int) pending, max));
  }

  /** Submit a new suggestion. Returns 201 with created entry id and status. */
  @PostMapping("/suggestions")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<SubmitSuggestionResponse> submitSuggestion(
      @RequestBody @Valid SubmitSuggestionRequest body) {

    var cmd =
        new SubmitTchalaSuggestionCommand(body.lang(), body.dream(), body.numbers(), body.note());
    SubmitTchalaSuggestionResult res = commandBus.execute(cmd);

    var response =
        new SubmitSuggestionResponse(
            res.entryId(), res.status(), res.conflictsWithCanonical(), res.conflictWithEntryId());

    return ApiResponse.created(response);
  }

  record SuggestionStatusResponse(boolean open, int pendingCount, int maxPending) {}
}
