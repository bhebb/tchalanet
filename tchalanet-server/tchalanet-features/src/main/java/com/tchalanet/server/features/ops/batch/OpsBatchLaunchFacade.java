package com.tchalanet.server.features.ops.batch;

import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.job.launch.BatchJobStarter;
import com.tchalanet.server.common.job.params.JobParamKeys;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.features.ops.batch.model.OpsJobLaunchItem;
import com.tchalanet.server.features.ops.batch.model.OpsLaunchResponse;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsBatchLaunchFacade {

    private final BatchJobStarter batchJobStarter;
    private final TenantPreContextLookupApi tenantPreContextLookupApi;

    @Value("${tch.ops.batch.max-tenants-per-interactive-launch:50}")
    private int maxTenantsPerInteractiveLaunch;

    public OpsLaunchResponse launchGlobal(JobKey jobKey, Map<String, String> params) {
        var launches = new ArrayList<OpsJobLaunchItem>(1);
        try {
            var execution = batchJobStarter.start(jobKey, cleanParams(params));
            launches.add(new OpsJobLaunchItem(null, parseExecutionId(execution.jobExecutionId()), execution.status(), null));
        } catch (Exception ex) {
            launches.add(new OpsJobLaunchItem(null, null, "FAILED_TO_START", rootMessage(ex)));
        }
        return response(jobKey, 1, launches);
    }

    public OpsLaunchResponse launchForTenants(
        JobKey jobKey,
        List<String> tenantCodes,
        Function<TenantId, Map<String, String>> paramsFactory
    ) {
        var tenants = resolveTargetTenants(tenantCodes);
        if (tenants.size() > maxTenantsPerInteractiveLaunch) {
            throw ProblemRest.badRequest(
                "too many tenants for interactive launch: " + tenants.size()
                    + " > " + maxTenantsPerInteractiveLaunch);
        }

        var launches = new ArrayList<OpsJobLaunchItem>(tenants.size());
        for (TenantId tenantId : tenants) {
            var params = new LinkedHashMap<>(paramsFactory.apply(tenantId));
            params.put(JobParamKeys.TENANT_ID, tenantId.value().toString());
            try {
                var execution = batchJobStarter.start(jobKey, cleanParams(params));
                launches.add(new OpsJobLaunchItem(
                    tenantId.value().toString(),
                    parseExecutionId(execution.jobExecutionId()),
                    execution.status(),
                    null));
            } catch (Exception ex) {
                launches.add(new OpsJobLaunchItem(
                    tenantId.value().toString(),
                    null,
                    "FAILED_TO_START",
                    rootMessage(ex)));
            }
        }
        return response(jobKey, tenants.size(), launches);
    }

    private List<TenantId> resolveTargetTenants(List<String> tenantCodes) {
        if (tenantCodes == null || tenantCodes.isEmpty()) {
            return tenantPreContextLookupApi.listActiveTenantIds();
        }
        return tenantCodes.stream()
            .filter(code -> code != null && !code.isBlank())
            .map(code -> tenantPreContextLookupApi.findByCode(code.trim().toLowerCase())
                .map(TenantContextLookupView::tenantId)
                .orElseThrow(() -> ProblemRest.badRequest("Unknown tenant code: " + code)))
            .toList();
    }

    private static OpsLaunchResponse response(JobKey jobKey, int requested, List<OpsJobLaunchItem> launches) {
        int failed = (int) launches.stream().filter(item -> item.error() != null).count();
        int started = launches.size() - failed;
        return new OpsLaunchResponse(
            jobKey.value(),
            requested,
            started,
            failed,
            List.copyOf(launches),
            started + "/" + requested + " job launch(es) started");
    }

    private static Map<String, String> cleanParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return Map.of();
        }
        var cleaned = new LinkedHashMap<String, String>();
        params.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null || value.isBlank()) {
                return;
            }
            cleaned.put(key, value);
        });
        return cleaned;
    }

    private static Long parseExecutionId(String raw) {
        return raw == null || raw.isBlank() ? null : Long.parseLong(raw);
    }

    private static String rootMessage(Throwable ex) {
        Throwable cur = ex;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur.getMessage() != null ? cur.getMessage() : ex.getMessage();
    }
}
