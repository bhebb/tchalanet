package com.tchalanet.server.common.batch.gate;

import com.tchalanet.server.common.batch.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.Optional;

/**
 * Cache interface for batch gate enable/disable flags.
 *
 * Abstracts L1/L2 caching strategy.
 * Implementations can use Caffeine+Redis or simple in-memory.
 */
public interface BatchGateCache {

    /**
     * Get tenant-specific override for a job.
     *
     * @param jobKey the job key
     * @param tenantId the tenant ID
     * @return Optional containing the flag if cached, empty if not cached
     */
    Optional<Boolean> getTenant(JobKey jobKey, TenantId tenantId);

    /**
     * Get global flag for a job.
     *
     * @param jobKey the job key
     * @return Optional containing the flag if cached, empty if not cached
     */
    Optional<Boolean> getGlobal(JobKey jobKey);

    /**
     * Put tenant-specific override in cache.
     *
     * @param jobKey the job key
     * @param tenantId the tenant ID
     * @param enabled the flag value
     */
    void putTenant(JobKey jobKey, TenantId tenantId, boolean enabled);

    /**
     * Put global flag in cache.
     *
     * @param jobKey the job key
     * @param enabled the flag value
     */
    void putGlobal(JobKey jobKey, boolean enabled);
}
