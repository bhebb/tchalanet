import { Application, Request, Response } from 'express';
import { DecisionSet } from '../core/decision-types';
import { evaluateDecisionSet } from '../core/rule-engine';

export function registerRuleRoutes(app: Application) {
  app.post('/api/rules/validate', (req: Request, res: Response) => {
    const decisionSet = req.body.decisionSet as DecisionSet | undefined;
    const errors: string[] = [];

    if (!decisionSet) {
      errors.push('decisionSet is required');
    } else {
      if (!decisionSet.name) errors.push('decisionSet.name is required');
      if (!Array.isArray(decisionSet.decisions) || decisionSet.decisions.length === 0) {
        errors.push('decisionSet.decisions must be a non-empty array');
      }
    }

    if (errors.length) res.status(400).json({ valid: false, errors });
    else res.json({ valid: true, errors: [] });
  });

  app.post('/api/rules/evaluate', async (req: Request, res: Response) => {
    const decisionSet = req.body.decisionSet as DecisionSet | undefined;
    const facts = req.body.facts ?? {};
    if (!decisionSet) return res.status(400).json({ error: 'decisionSet is required' });
    try {
      const events = await evaluateDecisionSet(decisionSet, facts);
      res.json({ matched: events.length > 0, events });
    } catch (err: any) {
      res.status(500).json({ error: err?.message || String(err) });
    }
  });
}
