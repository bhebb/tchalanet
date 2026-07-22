package com.tchalanet.server.features.pos.profile.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePosProfileTerminalRequest(@NotBlank @Size(max = 180) String displayName) {}
