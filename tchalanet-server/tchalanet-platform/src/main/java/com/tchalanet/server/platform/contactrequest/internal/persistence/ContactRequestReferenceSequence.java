package com.tchalanet.server.platform.contactrequest.internal.persistence;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContactRequestReferenceSequence {

  private final JdbcTemplate jdbc;

  public long nextValue() {
    Long value = jdbc.queryForObject("SELECT nextval('contact_request_ref_seq')", Long.class);
    return Objects.requireNonNull(value, "contact request reference sequence returned null");
  }
}
