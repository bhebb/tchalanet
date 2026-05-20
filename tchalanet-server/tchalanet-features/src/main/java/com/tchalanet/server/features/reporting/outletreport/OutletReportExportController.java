package com.tchalanet.server.features.reporting.outletreport;

import com.tchalanet.server.common.context.web.CurrentContext;

import com.tchalanet.server.common.context.TchRequestContext;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/reports/outlet")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@Tag(name = "Tenant • Outlet Reports")
public class OutletReportExportController {

  private final OutletReportExportService reportExportService;

  @Operation(summary = "Generate outlet report")
  @GetMapping("/{id}/export")
  public ApiResponse<String> export(
      @CurrentContext TchRequestContext ctx,
      @PathVariable OutletId id,
      @RequestParam("from") String from,
      @RequestParam("to") String to) {
    try {
      Path path =
          reportExportService.generate(
              ctx.tenantIdSafe(), id, LocalDate.parse(from), LocalDate.parse(to));
      return ApiResponse.success(path.toString());
    } catch (DateTimeParseException e) {
      throw ProblemRest.badRequest("invalid date format: " + from + " / " + to);
    }
  }

  @Operation(summary = "Download outlet report")
  @GetMapping("/{id}/download")
  public ResponseEntity<Resource> download(
      @CurrentContext TchRequestContext ctx,
      @PathVariable OutletId id,
      @RequestParam("date") String date) {
    LocalDate parsedDate;
    try {
      parsedDate = LocalDate.parse(date);
    } catch (DateTimeParseException e) {
      throw ProblemRest.badRequest("invalid date format: " + date);
    }

    Path path = reportExportService.generate(ctx.tenantIdSafe(), id, parsedDate, parsedDate);
    if (path == null || !Files.exists(path)) {
      throw ProblemRest.notFound("outlet report not found", id);
    }

    try {
      byte[] bytes = Files.readAllBytes(path);
      ByteArrayResource resource = new ByteArrayResource(bytes);
      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"outlet-report-" + parsedDate + ".csv\"")
          .contentLength(bytes.length)
          .contentType(MediaType.parseMediaType("text/csv"))
          .body(resource);
    } catch (IOException e) {
      throw ProblemRest.internal("failed to download outlet report");
    } finally {
      try {
        Files.deleteIfExists(path);
      } catch (IOException ignored) {
      }
    }
  }
}
