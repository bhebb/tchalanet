import { Liquid } from 'liquidjs';

const engine = new Liquid();

export async function renderTemplate(templateString: string, context: any): Promise<string> {
  return engine.parseAndRender(templateString, context);
}
