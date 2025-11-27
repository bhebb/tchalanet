import { Application, Request, Response } from 'express';
import fs from 'fs/promises';
import path from 'path';
import { renderTemplate } from '../core/template-engine';

export function registerPreviewRoutes(app: Application) {
  app.post('/api/preview/notification', async (req: Request, res: Response) => {
    const { templateId, context } = req.body;
    if (!templateId) {
      return res.status(400).json({ error: 'templateId is required' });
    }
    try {
      const fullPath = path.resolve(process.cwd(), 'templates', `${templateId}.liquid`);
      const tpl = await fs.readFile(fullPath, 'utf-8');
      const rendered = await renderTemplate(tpl, context || {});
      res.json({ rendered, templateId });
    } catch (err: any) {
      res.status(500).json({ error: err?.message || String(err) });
    }
  });
}
