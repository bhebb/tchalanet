package com.tchalanet.server.platform.identity.api.model.request;

import com.tchalanet.server.common.types.id.UserId;
import java.util.Locale;
import java.util.Optional;

public record UpdateUserProfileRequest(
    UserId userId,
    Optional<String> firstName,
    Optional<String> lastName,
    Optional<String> email,
    Optional<String> phone,
    Optional<Locale> locale) {

  public UpdateUserProfileRequest {
    firstName = firstName == null ? Optional.empty() : firstName;
    lastName = lastName == null ? Optional.empty() : lastName;
    email = email == null ? Optional.empty() : email;
    phone = phone == null ? Optional.empty() : phone;
    locale = locale == null ? Optional.empty() : locale;
  }
}
