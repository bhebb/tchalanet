import { Application, Request, Response } from 'express';
import { ChannelType, sendNotification } from '../core/communication-handler';

export function registerCommunicationRoutes(app: Application) {
  app.post('/api/communications/send', async (req: Request, res: Response) => {
    const { tenantId, type, templateId, recipients, context, options } = req.body;
    if (!tenantId || !type || !templateId) {
      return res.status(400).json({ error: 'tenantId, type and templateId are required' });
    }

    try {
      const results = await sendNotification({
        tenantId,
        channel: type as ChannelType,
        templateId,
        recipients: recipients || [],
        context: context || {},
        options
      });
      res.json({ status: 'ok', type, templateId, tenantId, results });
    } catch (err: any) {
      res.status(500).json({ status: 'error', error: err?.message || String(err) });
    }
  });
}
