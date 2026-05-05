package com.tchalanet.server.core.drawresult.application.service;

import com.tchalanet.server.common.contracts.haiti.HaitiFlags;
import tools.jackson.databind.node.ObjectNode;

public record HaitiProjectionResult(ObjectNode haitiResult, HaitiFlags flags) {}
