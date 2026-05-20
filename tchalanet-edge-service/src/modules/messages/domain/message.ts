export type MessageChannel = 'SLACK' | 'EMAIL' | 'SMS' | 'WHATSAPP';
export type MessageSeverity = 'INFO' | 'WARN' | 'ERROR' | 'CRITICAL';

export interface MessageRecipient {
  channel: MessageChannel;
  to?: string;
  channelKey?: string;
}

export interface SendMessageRequest {
  eventId: string;
  tenantCode?: string;
  severity: MessageSeverity;
  title: string;
  message: string;
  recipients: MessageRecipient[];
  context?: Record<string, unknown>;
}

export interface MessageDeliveryResult {
  channel: MessageChannel;
  to?: string;
  channelKey?: string;
  accepted: boolean;
  reason?: string;
}

export interface SendMessageResponse {
  accepted: boolean;
  eventId: string;
  deliveries: MessageDeliveryResult[];
}
