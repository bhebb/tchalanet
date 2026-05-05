import type { FastifyInstance } from 'fastify';

import type { TicketDeliveryService } from '../application/ticket-delivery.service.js';
import type { TicketDeliveryRequest, TicketDeliveryResponse } from '../domain/delivery-request.js';

import { deliverTicketSchema } from './delivery.schemas.js';

export function deliveryRoutes(service: TicketDeliveryService) {
  return async function (app: FastifyInstance): Promise<void> {
    app.post<{ Body: TicketDeliveryRequest; Reply: TicketDeliveryResponse }>(
      '/internal/delivery/ticket',
      { schema: deliverTicketSchema },
      async (request, reply) => {
        const result = await service.deliver(request.body);
        return reply.status(202).send(result);
      },
    );
  };
}
