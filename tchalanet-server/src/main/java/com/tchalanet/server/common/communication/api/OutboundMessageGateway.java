package com.tchalanet.server.common.communication.api;

/** Gateway for external outbound communication such as Slack, email, SMS, and WhatsApp. */
public interface OutboundMessageGateway {

    void send(OutboundMessageRequest request);
}
