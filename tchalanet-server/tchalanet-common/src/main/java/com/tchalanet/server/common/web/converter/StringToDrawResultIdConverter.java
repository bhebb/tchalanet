package com.tchalanet.server.common.web.converter;

import com.tchalanet.server.common.types.id.DrawResultId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToDrawResultIdConverter implements Converter<String, DrawResultId> {

    @Override
    public DrawResultId convert(String source) {
        if (source == null || source.isBlank()) return null;
        return DrawResultId.parse(source);
    }
}
