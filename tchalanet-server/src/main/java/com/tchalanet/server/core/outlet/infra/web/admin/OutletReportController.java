package com.tchalanet.server.core.outlet.infra.web.admin;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.core.outlet.application.query.model.GenerateOutletReportQuery;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletDailySummaryQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletDailySummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * TODO (ARCH):
 * - This reporting endpoint is TEMPORARY.
 * - Reports will be moved to a dedicated Dashboard feature
 *   (e.g. features.dashboard.reports) and integrated into UI dashboards.
 * - This controller should be removed once dashboards are implemented.
 *
 * Reason:
 * - Reports are a READ / ANALYTICS concern, not an admin CRUD concern.
 */

@RestController
@RequestMapping("/platform/outlets")
@RequiredArgsConstructor
@Tags({@Tag(name = "Platform • Outlets")})
public class OutletReportController {

    private final QueryBus queryBus;


    @Operation(summary = "Get outlet daily summary (admin)")
    @GetMapping("/{id}/daily-summary")
    public ResponseEntity<OutletDailySummary> dailySummary(
        @CurrentContext TchRequestContext ctx,
        @PathVariable OutletId id,
        @RequestParam("date") String date) {
        var tenantId = ctx.tenantIdSafe();
        var summary = queryBus.send(new GetOutletDailySummaryQuery(tenantId, id, LocalDate.parse(date)));
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Generate outlet report (admin)")
    @GetMapping("/{id}/report")
    public ResponseEntity<String> generateReport(
        @CurrentContext TchRequestContext ctx,
        @PathVariable OutletId id,
        @RequestParam("from") String from,
        @RequestParam("to") String to) {
        var tenantId = ctx.tenantIdSafe();
        var path =
            queryBus.send(
                new GenerateOutletReportQuery(tenantId, id, LocalDate.parse(from), LocalDate.parse(to)));
        return ResponseEntity.ok(path.toString());
    }

    @Operation(summary = "Download outlet report (admin)")
    @GetMapping("/{id}/report/download")
    public ResponseEntity<Resource> downloadReport(
        @CurrentContext TchRequestContext ctx,
        @PathVariable OutletId id,
        @RequestParam("date") String date) {
        LocalDate d;
        try {
            d = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }

        var tenantId = ctx.tenantIdSafe();
        Path path = queryBus.send(new GenerateOutletReportQuery(tenantId, id, d, d));
        if (path == null || !Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        try {
            InputStreamResource resource = new InputStreamResource(Files.newInputStream(path));
            String filename = "outlet-report-" + d + ".csv";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(Files.size(path))
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

}
