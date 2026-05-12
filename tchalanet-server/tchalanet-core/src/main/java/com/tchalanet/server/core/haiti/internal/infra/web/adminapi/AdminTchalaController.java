package com.tchalanet.server.core.haiti.internal.infra.web.adminapi;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.apiresponse.ApiResponse;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.core.haiti.application.command.model.ApproveTchalaEntryCommand;
import com.tchalanet.server.core.haiti.application.command.model.DeleteTchalaEntriesCommand;
import com.tchalanet.server.core.haiti.application.command.model.ImportTchalaEntriesCommand;
import com.tchalanet.server.core.haiti.application.command.model.ImportTchalaReport;
import com.tchalanet.server.core.haiti.application.command.model.MergeTchalaEntriesCommand;
import com.tchalanet.server.core.haiti.application.command.model.RejectTchalaEntryCommand;
import com.tchalanet.server.core.haiti.application.query.model.ListPendingTchalaEntriesQuery;
import com.tchalanet.server.core.haiti.infra.web.model.ApproveRequest;
import com.tchalanet.server.core.haiti.infra.web.model.ApproveResponse;
import com.tchalanet.server.core.haiti.infra.web.model.DeleteRequest;
import com.tchalanet.server.core.haiti.infra.web.model.DeleteResponse;
import com.tchalanet.server.core.haiti.infra.web.model.MergeRequest;
import com.tchalanet.server.core.haiti.infra.web.model.MergeResponse;
import com.tchalanet.server.core.haiti.infra.web.model.RejectRequest;
import com.tchalanet.server.core.haiti.infra.web.model.TchalaEntryResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

// import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Admin REST API for managing Tchala entries: moderation, merge and import.
 *
 * <p>Endpoints are protected (in production) — controller delegates to CommandBus/QueryBus.
 */
@Validated
@RestController
@RequestMapping("/admin/tchala")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Admin • Tchala")
public class AdminTchalaController {

  private final QueryBus queryBus;
  private final CommandBus commandBus;

  public AdminTchalaController(QueryBus queryBus, CommandBus commandBus) {
    this.queryBus = queryBus;
    this.commandBus = commandBus;
  }

  // @PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN')")
  @GetMapping("/pending")
  public TchPage<TchalaEntryResponse> pending(
      @RequestParam(defaultValue = "fr") String lang,
      @RequestParam(defaultValue = "false") boolean conflictOnly,
      @RequestParam(defaultValue = "0") @Min(0) int offset,
      @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {

    int pageSize = limit;
    int page = offset / pageSize;

    var q = new ListPendingTchalaEntriesQuery(lang, conflictOnly, page, pageSize);
    var res = queryBus.ask(q);

    return TchPage.of(
        res.items().stream().map(TchalaEntryResponse::from).toList(),
        res.page(),
        res.size(),
        res.totalElements(),
        res.totalPages(),
        res.last(),
        res.hasNext(),
        res.hasPrevious());
  }

  // @PreAuthorize("hasRole('SUPERADMIN')")
  @PostMapping("/approve")
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<ApproveResponse> approve(@RequestBody @Valid ApproveRequest body) {
    var cmd =
        new ApproveTchalaEntryCommand(
            body.entryId(),
            body.mode(),
            java.util.Optional.ofNullable(body.targetCanonicalId()),
            body.mergePolicy());
    UUID canonicalId = commandBus.execute(cmd);
    return ApiResponse.success(new ApproveResponse(canonicalId));
  }

  // @PreAuthorize("hasRole('SUPERADMIN')")
  @PostMapping("/reject")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ApiResponse<Void> reject(@RequestBody @Valid RejectRequest body) {
    commandBus.execute(new RejectTchalaEntryCommand(body.entryId(), body.reason()));
    return ApiResponse.success(null);
  }

  // @PreAuthorize("hasRole('SUPERADMIN')")
  @PostMapping("/merge")
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<MergeResponse> merge(@RequestBody @Valid MergeRequest body) {
    UUID canonicalId =
        commandBus.execute(
            new MergeTchalaEntriesCommand(
                body.fromEntryId(), body.intoEntryId(), body.mergePolicy()));
    return ApiResponse.success(new MergeResponse(canonicalId));
  }

  @PostMapping(value = "/import", consumes = "multipart/form-data")
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<ImportTchalaReport>
      uploadImport(
          @RequestParam(defaultValue = "fr") String lang,
          @RequestParam ImportTchalaEntriesCommand.ImportMode mode,
          @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {

    if (file == null || file.isEmpty()) throw new IllegalArgumentException("file is empty");

    java.nio.file.Path tempFilePath;
    try {
      tempFilePath = java.nio.file.Files.createTempFile("tchala-import-", ".csv");
      file.transferTo(tempFilePath);
    } catch (Exception e) {
      throw new IllegalStateException("failed to store upload: " + e.getMessage(), e);
    }

    try {
      var cmd = new ImportTchalaEntriesCommand(lang, tempFilePath.toString(), mode);
      var report = commandBus.execute(cmd);
      return ApiResponse.success(report);
    } finally {
      try {
        java.nio.file.Files.deleteIfExists(tempFilePath);
      } catch (Exception ignored) {
      }
    }
  }

  // Admin delete endpoint (hard delete)
  @PostMapping("/delete")
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<DeleteResponse> deleteEntries(@RequestBody @Valid DeleteRequest body) {
    var cmd = new DeleteTchalaEntriesCommand(body.entryIds());
    commandBus.execute(cmd);
    return ApiResponse.success(new DeleteResponse(body.entryIds().size()));
  }
}
