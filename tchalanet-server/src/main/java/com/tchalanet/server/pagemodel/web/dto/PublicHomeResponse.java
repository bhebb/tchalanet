package com.tchalanet.server.pagemodel.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record PublicHomeResponse(String code, String lang, JsonNode model) {}
