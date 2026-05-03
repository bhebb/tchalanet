import Fastify from 'fastify';

import { healthRoutes } from './modules/health/health.routes.js';
import { pingRoutes } from './modules/ping/ping.routes.js';
import { errorHandlerPlugin } from './plugins/error-handler.plugin.js';

export function buildApp() {
  const app = Fastify({ logger: true });

  app.register(errorHandlerPlugin);
  app.register(pingRoutes);
  app.register(healthRoutes);

  return app;
}
