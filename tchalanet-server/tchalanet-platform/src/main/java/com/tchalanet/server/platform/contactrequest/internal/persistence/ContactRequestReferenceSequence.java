package com.tchalanet.server.platform.contactrequest.internal.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContactRequestReferenceSequence {

    private final JdbcTemplate jdbc;

    public long nextValue() {
        return jdbc.queryForObject(
            "SELECT nextval('contact_request_ref_seq')",
            Long.class);
    }
}
