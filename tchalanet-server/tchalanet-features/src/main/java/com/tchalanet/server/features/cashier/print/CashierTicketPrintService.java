package com.tchalanet.server.features.cashier.print;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.command.print.RecordTicketPrintCommand;
import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.query.GetTicketPrintViewQuery;
import com.tchalanet.server.features.cashier.print.mapper.TicketPrintCommunicationMapper;
import com.tchalanet.server.features.cashier.print.mapper.TicketPrintDocumentMapper;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.document.api.DocumentApi;
import com.tchalanet.server.platform.document.api.model.DocumentFormat;
import com.tchalanet.server.platform.document.api.model.RenderedDocument;
import com.tchalanet.server.platform.tenantconfig.api.model.view.TenantInternalDocumentConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashierTicketPrintService {

    private final QueryBus queryBus;
    private final CommandBus commandBus;
    private final DocumentApi documentApi;
    private final CommunicationApi communicationApi;
    private final TicketPrintDocumentMapper documentMapper;
    private final TicketPrintCommunicationMapper communicationMapper;
    private final TenantDocumentConfigAdapter tenantDocumentConfigAdapter;

    public ResponseEntity<ByteArrayResource> printTicket(
        TchRequestContext ctx,
        TicketId ticketId,
        PrintTicketRequest request
    ) {
        var printView = queryBus.ask(new GetTicketPrintViewQuery(ticketId));
        var receiptConfig = tenantDocumentConfigAdapter.resolveReceiptConfig(ctx);

        var renderRequest = documentMapper.toRenderRequest(printView, request, receiptConfig);
        var rendered = documentApi.render(renderRequest);

        if (request.recordPrint()) {
            commandBus.execute(new RecordTicketPrintCommand(
                ticketId,
                request.format(),
                request.reprintReason()
            ));
        }

        if (request.shouldSendToBuyer()) {
            var outboundDocument = toCommunicationDocument(printView, request, rendered, receiptConfig);
            communicationMapper.toOutboundMessages(ctx, printView, outboundDocument, request)
                .forEach(communicationApi::enqueue);
        }

        return toFileResponse(rendered);
    }

    private ResponseEntity<ByteArrayResource> toFileResponse(RenderedDocument document) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(document.contentType()));
        headers.setCacheControl(CacheControl.noStore());
        headers.setContentDisposition(
            ContentDisposition.inline()
                .filename(document.filename())
                .build()
        );

        return new ResponseEntity<>(
            new ByteArrayResource(document.bytes()),
            headers,
            HttpStatus.OK
        );
    }

    private RenderedDocument toCommunicationDocument(
        TicketPrintView printView,
        PrintTicketRequest request,
        RenderedDocument rendered,
        TenantInternalDocumentConfig.ReceiptConfig receiptConfig
    ) {
        if (rendered.format() == DocumentFormat.PDF) {
            return rendered;
        }

        var pdfRequest = new PrintTicketRequest(
            PrintOutputFormat.PDF,
            false,
            request.reprintReason(),
            request.deliveryOptions(),
            request.buyerPhoneNumber(),
            request.buyerEmail(),
            request.buyerLocale()
        );

        return documentApi.render(documentMapper.toRenderRequest(printView, pdfRequest, receiptConfig));
    }
}
