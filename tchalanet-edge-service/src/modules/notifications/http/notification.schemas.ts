export const sendNotificationSchema = {
  body: {
    type: 'object',
    required: ['eventId', 'severity', 'title', 'message', 'recipients'],
    additionalProperties: false,
    properties: {
      eventId: { type: 'string' },
      tenantCode: { type: 'string' },
      severity: { type: 'string', enum: ['INFO', 'WARN', 'ERROR', 'CRITICAL'] },
      title: { type: 'string' },
      message: { type: 'string' },
      recipients: {
        type: 'array',
        minItems: 1,
        items: {
          type: 'object',
          required: ['channel'],
          additionalProperties: false,
          properties: {
            channel: { type: 'string', enum: ['SLACK', 'EMAIL', 'SMS', 'WHATSAPP'] },
            to: { type: 'string' },
            channelKey: { type: 'string' },
          },
        },
      },
      context: { type: 'object', additionalProperties: true },
    },
  },
} as const;
