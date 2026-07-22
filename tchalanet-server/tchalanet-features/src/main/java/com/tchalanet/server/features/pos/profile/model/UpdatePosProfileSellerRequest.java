package com.tchalanet.server.features.pos.profile.model;

import com.tchalanet.server.common.types.id.AddressId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdatePosProfileSellerRequest(
    @Size(max = 120) String firstName,
    @Size(max = 120) String lastName,
    @Email @Size(max = 254) String email,
    @Size(max = 64) String phoneNumber,
    AddressId addressId) {}
