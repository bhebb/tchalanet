export type DeliveryChannel = 'EMAIL' | 'SMS' | 'WHATSAPP';

export interface DeliveryLine {
  gameCode: string;
  selection: string;
  stake: number;
  potentialPayout?: number;
}

export interface TicketDeliveryRequest {
  requestId: string;
  channel: DeliveryChannel;
  recipient: string;
  locale?: string;
  includePdf?: boolean;
  includeVerificationLink?: boolean;
  ticketCode: string;
  publicCode: string;
  verificationUrl?: string;
  totalAmount?: number;
  currency?: string;
  soldAt?: string;
  outletName?: string;
  drawChannelLabel?: string;
  drawWhenLabel?: string;
  lines?: DeliveryLine[];
}

export interface TicketDeliveryResponse {
  requestId: string;
  accepted: boolean;
  channel: DeliveryChannel;
  reason?: string;
}
