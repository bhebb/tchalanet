package com.tchalanet.server.core.draw.api;

import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.draw.api.query.DrawResultAffectedTenant;
import java.time.LocalDate;
import java.util.List;

public interface DrawResultAffectedTenantApi {

  List<DrawResultAffectedTenant> listAffectedTenants(ResultSlotId resultSlotId, LocalDate drawDate);
}
