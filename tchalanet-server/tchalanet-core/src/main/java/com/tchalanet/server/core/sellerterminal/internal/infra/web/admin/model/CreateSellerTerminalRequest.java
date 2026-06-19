package com.tchalanet.server.core.sellerterminal.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.AddressId;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateSellerTerminalRequest(
    @NotBlank @Size(max = 64) String terminalCode,
    @NotBlank @Size(max = 180) String displayName,
    @Size(max = 120) String firstName,
    @Size(max = 120) String lastName,
    @Size(max = 64) String phoneNumber,
    AddressId addressId,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal commissionRate,
    @Pattern(regexp = "\\d{4,8}", message = "PIN must be 4–8 digits") String initialPin
) {}
