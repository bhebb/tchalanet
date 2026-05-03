import type { FastifyInstance } from 'fastify';

import { AppError } from '../common/errors/app-error.js';

export async function errorHandlerPlugin(app: FastifyInstance): Promise<void> {
  app.setErrorHandler((error, _request, reply) => {
    if (error instanceof AppError) {
      return reply.status(error.statusCode).send({ error: error.message });
    }
    app.log.error(error);
    return reply.status(500).send({ error: 'Internal Server Error' });
  });
}
