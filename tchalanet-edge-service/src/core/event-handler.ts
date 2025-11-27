import { loadDecisionSetForEvent, evaluateDecisionSet } from './rule-engine';
import { sendNotification, NotificationResult } from './communication-handler';

export interface IncomingEventPayload {
  eventType: string;
  eventId?: string;
  tenantId: string;
  occurredAt?: string;
  context: any;
  meta?: {
    source?: string;
    idempotencyKey?: string;
  };
}

export interface EventHandlingResult {
  status: 'ok' | 'ignored' | 'error';
  eventType: string;
  tenantId: string;
  actions: {
    type: string;
    channel?: string;
    provider?: string;
    templateId?: string;
    results?: NotificationResult[];
  }[];
  error?: string;
}

export async function handleIncomingEvent(payload: IncomingEventPayload): Promise<EventHandlingResult> {
  const { eventType, tenantId, context } = payload;

  const decisionSet = await loadDecisionSetForEvent(eventType);
  if (!decisionSet) {
    return { status: 'ignored', eventType, tenantId, actions: [] };
  }

  const facts = { eventType, tenantId, ...context };
  const events = await evaluateDecisionSet(decisionSet, facts);
  const actions: EventHandlingResult['actions'] = [];

  for (const ev of events) {
    if (ev.type === 'send_notification') {
      const channel = ev.params?.channel as string;
      const provider = (ev.params?.provider as string) || undefined;
      const templateId = ev.params?.templateId as string;

      const recipients =
        (context?.recipients as any[]) ||
        (context?.user?.phone || context?.user?.email
          ? [{ to: context.user.phone || context.user.email }]
          : []);

      const results = await sendNotification({
        tenantId,
        channel: channel as any,
        provider,
        templateId,
        recipients,
        context
      });

      actions.push({ type: 'send_notification', channel, provider, templateId, results });
    } else {
      actions.push({ type: ev.type as string });
    }
  }

  return { status: 'ok', eventType, tenantId, actions };
}
