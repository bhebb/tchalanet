import { Engine } from 'json-rules-engine';
import { DecisionSet } from './decision-types';
import fs from 'fs/promises';
import path from 'path';

export async function evaluateDecisionSet(decisionSet: DecisionSet, facts: any) {
  const engine = new Engine();

  for (const decision of decisionSet.decisions) {
    engine.addRule({
      name: decisionSet.name,
      conditions: decision.conditions,
      event: decision.event
    });
  }

  const { events } = await engine.run(facts);
  return events;
}

export async function loadDecisionSetForEvent(eventType: string): Promise<DecisionSet | null> {
  const fullPath = path.resolve(process.cwd(), 'rules', `${eventType}.json`);
  try {
    const content = await fs.readFile(fullPath, 'utf-8');
    return JSON.parse(content) as DecisionSet;
  } catch {
    return null;
  }
}
