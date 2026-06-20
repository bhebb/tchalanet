package com.tchalanet.server.common.context.operational;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class OperationalContextHeaderParserTest {

    @Test
    void noHeadersReturnsNoContext() {
        var hint = OperationalContextHeaderParser.parseHint(Map.<String, String>of()::get);

        assertThat(hint.source()).isEqualTo(OperationalContextSource.NONE);
        assertThat(hint.trust()).isEqualTo(OperationalContextTrust.NONE);
    }



    private static OperationalContextHeaderParser.HeaderReader headers(String... pairs) {
        var values = new java.util.HashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put(pairs[i], pairs[i + 1]);
        }
        return values::get;
    }
}
