package com.tchalanet.server.core.sales.internal.application.receipt;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.sale.TicketBackupInfo;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver;
import org.springframework.stereotype.Component;

@Component
public class TicketBackupAssembler {

    private final TicketReceiptMessageFormatter messageFormatter;
    private final TicketReceiptI18nResolver i18nResolver;

    public TicketBackupAssembler(
        TicketReceiptMessageFormatter messageFormatter,
        TicketReceiptI18nResolver i18nResolver
    ) {
        this.messageFormatter = messageFormatter;
        this.i18nResolver = i18nResolver;
    }

    public TicketBackupInfo assemble(TicketReceiptView receipt) {
        var translations = i18nResolver.resolve(receipt.locale(), receipt.tenantId());

        var shareable = messageFormatter.formatShareableText(receipt);

        var codeTpl = translations.text(TicketReceiptI18nKeys.MESSAGE_BACKUP_CODE);
        var codeMsg = formatOrFallback(codeTpl, "Votre code est {code}.", "{code}", receipt.displayCode());

        var verifyTpl = translations.text(TicketReceiptI18nKeys.MESSAGE_BACKUP_VERIFY);
        var verifyMsg = formatOrFallback(verifyTpl, "Verifier sur {url}", "{url}", receipt.verificationUrl());

        return new TicketBackupInfo(
            receipt.displayCode(),
            receipt.verificationUrl(),
            shareable,
            codeMsg,
            verifyMsg
        );
    }

    private String formatOrFallback(String tpl, String fallback, String placeholder, String value) {
        if (tpl == null || tpl.isBlank() || tpl.equals(placeholder)) {
            return fallback.replace(placeholder, value == null ? "" : value);
        }
        if (tpl.contains(placeholder)) {
            return tpl.replace(placeholder, value == null ? "" : value);
        }
        // if template present but doesn't contain placeholder, append value
        return tpl + " " + (value == null ? "" : value);
    }
}
