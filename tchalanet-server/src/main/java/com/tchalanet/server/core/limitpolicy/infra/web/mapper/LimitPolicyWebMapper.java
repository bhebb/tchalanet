package com.tchalanet.server.core.limitpolicy.infra.web.mapper;

import com.tchalanet.server.core.limitpolicy.web.dto.LimitPolicyRequest;
import com.tchalanet.server.core.limitpolicy.web.dto.LimitPolicyResponse;
import org.springframework.stereotype.Component;

@Component
public class LimitPolicyWebMapper {
    public LimitPolicyResponse toResponse(Object o) { return new LimitPolicyResponse(); }
    public Object toDomain(LimitPolicyRequest r) { return new Object(); }
}

