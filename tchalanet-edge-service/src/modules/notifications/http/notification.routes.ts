import type { FastifyInstance } from 'fastify';

import type { SendNotificationService } from '../application/send-notification.service.js';
import type {
  SendNotificationRequest,
  SendNotificationResponse,
} from '../domain/notification-message.js';

import { sendNotificationSchema } from './notification.schemas.js';

// TODO: protect /internal/* with HMAC authentication in a follow-up change

export function notificationRoutes(service: SendNotificationService) {
  return async function (app: FastifyInstance): Promise<void> {
    app.post<{ Body: SendNotificationRequest; Reply: SendNotificationResponse }>(
      '/internal/notifications/send',
      { schema: sendNotificationSchema },
      async (request, reply) => {
        const result = await service.send(request.body);
        return reply.status(202).send(result);
      },
    );
  };
}
