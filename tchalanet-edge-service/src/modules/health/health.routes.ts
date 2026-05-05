import type { FastifyInstance } from 'fastify';

import type { HealthResponse, ReadyResponse } from './health.types.js';

export async function healthRoutes(app: FastifyInstance): Promise<void> {
  app.get<{ Reply: HealthResponse }>('/health', async () => {
    return { status: 'UP', service: 'tchalanet-edge-service' };
  });

  app.get<{ Reply: ReadyResponse }>('/ready', async () => {
    return { status: 'READY', service: 'tchalanet-edge-service' };
  });
}
