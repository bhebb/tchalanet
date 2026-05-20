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
        return reply.status(202).send(result);
      },
    );
  };
}
