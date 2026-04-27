import { Application, Request, Response } from 'express';

export function registerHealthRoutes(app: Application) {
  app.get('/health', (_req: Request, res: Response) => {
    res.json({ status: 'ok', service: 'tchalanet-edge-service' });
  });
}
