import type { FastifySchema } from 'fastify';

export const deliverTicketSchema: FastifySchema = {
  body: {
    type: 'object',
    required: ['requestId', 'channel', 'recipient', 'ticketCode', 'publicCode'],
    properties: {
      requestId: { type: 'string' },
      channel: { type: 'string', enum: ['EMAIL', 'SMS', 'WHATSAPP'] },
      recipient: { type: 'string', minLength: 1 },
      locale: { type: 'string' },
      includePdf: { type: 'boolean' },
      includeVerificationLink: { type: 'boolean' },
      ticketCode: { type: 'string' },
      publicCode: { type: 'string' },
      verificationUrl: { type: 'string' },
      totalAmount: { type: 'number' },
      currency: { type: 'string' },
      soldAt: { type: 'string' },
      outletName: { type: 'string' },
      drawChannelLabel: { type: 'string' },
      drawWhenLabel: { type: 'string' },
      lines: {
        type: 'array',
        items: {
          type: 'object',
          required: ['gameCode', 'selection', 'stake'],
          properties: {
            gameCode: { type: 'string' },
            selection: { type: 'string' },
            stake: { type: 'number' },
            potentialPayout: { type: 'number' },
          },
        },
      },
    },
  },
  response: {
    202: {
      type: 'object',
      properties: {
        requestId: { type: 'string' },
        accepted: { type: 'boolean' },
        channel: { type: 'string' },
        reason: { type: 'string' },
      },
    },
  },
};
