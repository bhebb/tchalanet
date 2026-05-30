package com.tchalanet.server.platform.document.api;

import com.tchalanet.server.platform.document.api.model.PrintOptionsRequest;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.DocumentPrintProfile;
import com.tchalanet.server.platform.document.api.model.PaperSize;
import org.springframework.stereotype.Component;

@Component
public class DocumentPrintProfileResolver {

    public DocumentPrintProfile resolve(PrintOptionsRequest options) {
        var outputFormat = options == null || options.outputFormat() == null
            ? DocumentFormat.PDF
            : options.outputFormat();

        var paperSize = options == null || options.paperSize() == null
            ? PaperSize.A4
            : options.paperSize();

        return DocumentPrintProfile.of(outputFormat, paperSize);
    }
}


