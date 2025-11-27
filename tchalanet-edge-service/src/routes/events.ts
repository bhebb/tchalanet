import { Application, Request, Response } from 'express';
import { handleIncomingEvent, IncomingEventPayload } from '../core/event-handler';

export function registerEventRoutes(app: Application) {
  app.post('/api/events', async (req: Request, res: Response) => {
    const payload = req.body as IncomingEventPayload;
    if (!payload || !payload.eventType || !payload.tenantId) {
      return res.status(400).json({ error: 'eventType and tenantId are required' });
    }
    try {
      const result = await handleIncomingEvent(payload);
      res.json(result);
    } catch (err: any) {
      res.status(500).json({ status: 'error', error: err?.message || String(err) });
    }
  });
}
