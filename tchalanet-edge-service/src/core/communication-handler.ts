import fs from 'fs/promises';
import path from 'path';
import { renderTemplate } from './template-engine';
import { sendEmailWithMailgun } from '../channels/mailgun-email';
import { sendSmsWithBird } from '../channels/bird-sms';
import { sendWebMessage } from '../channels/web-message';

export type ChannelType = 'WEB' | 'SMS' | 'EMAIL' | 'WHATSAPP' | 'PUSH';

export interface Recipient {
  to: string;
  channelId?: string;
}

export interface SendNotificationCommand {
  tenantId: string;
  channel: ChannelType;
  provider?: string;
  templateId: string;
  recipients: Recipient[];
  context: any;
  options?: {
    dryRun?: boolean;
    trackDelivery?: boolean;
  };
}

export interface NotificationResult {
  recipient: string;
  provider?: string;
  messageId?: string;
  status: 'rendered' | 'queued' | 'sent' | 'failed';
  error?: string;
}

async function loadAndRenderTemplate(templateId: string, context: any): Promise<string> {
  const fullPath = path.resolve(process.cwd(), 'templates', `${templateId}.liquid`);
  const content = await fs.readFile(fullPath, 'utf-8');
  return renderTemplate(content, context);
}

export async function sendNotification(cmd: SendNotificationCommand): Promise<NotificationResult[]> {
  const results: NotificationResult[] = [];
  const rendered = await loadAndRenderTemplate(cmd.templateId, cmd.context);
  const recipients = cmd.recipients && cmd.recipients.length ? cmd.recipients : [{ to: 'web' }];

  for (const recipient of recipients) {
    try {
      if (cmd.channel === 'WEB') {
        const messageId = await sendWebMessage({
          tenantId: cmd.tenantId,
          payload: rendered
        });
        results.push({ recipient: recipient.to, status: 'rendered', messageId, provider: 'web' });
      } else if (cmd.channel === 'EMAIL') {
        if (cmd.options?.dryRun) {
          results.push({ recipient: recipient.to, status: 'rendered', provider: cmd.provider || 'mailgun' });
        } else {
          const messageId = await sendEmailWithMailgun({
            to: recipient.to,
            subject: cmd.context?.subject || 'Notification Tchalanet',
            html: rendered
          });
          results.push({ recipient: recipient.to, status: 'queued', messageId, provider: cmd.provider || 'mailgun' });
        }
      } else if (cmd.channel === 'SMS') {
        if (cmd.options?.dryRun) {
          results.push({ recipient: recipient.to, status: 'rendered', provider: cmd.provider || 'bird' });
        } else {
          const messageId = await sendSmsWithBird({ to: recipient.to, text: rendered });
          results.push({ recipient: recipient.to, status: 'queued', messageId, provider: cmd.provider || 'bird' });
        }
      } else {
        results.push({
          recipient: recipient.to,
          status: 'failed',
          error: `Channel ${cmd.channel} not yet implemented`
        });
      }
    } catch (err: any) {
      results.push({
        recipient: recipient.to,
        status: 'failed',
        error: err?.message || String(err),
        provider: cmd.provider
      });
    }
  }

  return results;
}
