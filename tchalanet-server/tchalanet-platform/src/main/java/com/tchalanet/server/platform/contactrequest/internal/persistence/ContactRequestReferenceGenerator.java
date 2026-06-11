package com.tchalanet.server.platform.contactrequest.internal.persistence;

import java.time.Clock;
import java.time.Year;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContactRequestReferenceGenerator {

    private final ContactRequestReferenceSequence sequence;
    private final Clock clock;

    public String nextReference() {
        int year = Year.now(clock).getValue() % 100;
        long value = sequence.nextValue();
        return "CT-%02d-%06d".formatted(year, value);
    }
}
