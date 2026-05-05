import type { FastifyInstance } from 'fastify';

import type { PingResponse } from './ping.types.js';

export async function pingRoutes(app: FastifyInstance): Promise<void> {
  app.get<{ Reply: PingResponse }>('/ping', async () => {
    return { ok: true, service: 'tchalanet-edge-service' };
  });
}
