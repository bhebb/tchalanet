import type { FastifyInstance } from 'fastify';

import type { SendMessageService } from '../application/send-message.service.js';
import type { SendMessageRequest, SendMessageResponse } from '../domain/message.js';

import { sendMessageSchema } from './message.schemas.js';
import { type MessageHmacOptions,messageHmacPreParsing } from './message-hmac.js';

export function messageRoutes(service: SendMessageService, hmac: MessageHmacOptions) {
  return async function (app: FastifyInstance): Promise<void> {
    app.addHook('preParsing', messageHmacPreParsing(hmac));

    app.post<{ Body: SendMessageRequest; Reply: SendMessageResponse }>(
      '/internal/messages/send',
      { schema: sendMessageSchema },
      async (request, reply) => {
        const result = await service.send(request.body);

        // A rejected delivery still answers 202, so without this the failure is
        // invisible in the edge logs (e.g. SLACK_WEBHOOK_NOT_CONFIGURED:<key>).
        const rejected = result.deliveries.filter(d => !d.accepted);
        if (rejected.length > 0) {
          request.log.warn(
            {
              eventId: result.eventId,
              rejected: rejected.map(d => ({
                channel: d.channel,
                channelKey: d.channelKey,
                reason: d.reason,
              })),
            },
            'message delivery rejected',
          );
        }

        return reply.status(202).send(result);
      },
    );
  };
}
