export const NODE_ENV = process.env['NODE_ENV'] ?? 'development';
export const HOST = process.env['HOST'] ?? '0.0.0.0';
export const PORT = parseInt(process.env['PORT'] ?? '3000', 10);
export const EDGE_HMAC_SECRET = process.env['EDGE_HMAC_SECRET'] ?? '';

// Slack
export const SLACK_ENABLED = process.env['SLACK_ENABLED'] === 'true';
export const SLACK_WEBHOOKS: Record<string, string | undefined> = {
  tchalanet: process.env['SLACK_WEBHOOK_TCHALANET'],
  'batch-draws': process.env['SLACK_WEBHOOK_BATCH_DRAWS'],
  delivery: process.env['SLACK_WEBHOOK_DELIVERY'],
  'ops-alerts': process.env['SLACK_WEBHOOK_OPS_ALERTS'],
  'security-audit': process.env['SLACK_WEBHOOK_SECURITY_AUDIT'],
};

// Email
export const EMAIL_ENABLED = process.env['EMAIL_ENABLED'] === 'true';
export const EMAIL_PROVIDER = process.env['EMAIL_PROVIDER'] ?? 'brevo';
export const BREVO_API_KEY = process.env['BREVO_API_KEY'] ?? '';
export const EMAIL_FROM_NAME = process.env['EMAIL_FROM_NAME'] ?? 'Tchalanet';
export const EMAIL_FROM_ADDRESS = process.env['EMAIL_FROM_ADDRESS'] ?? 'no-reply@example.com';

// SMS
export const SMS_ENABLED = process.env['SMS_ENABLED'] === 'true';
export const SMS_PROVIDER = process.env['SMS_PROVIDER'] ?? 'twilio';
export const TWILIO_ACCOUNT_SID = process.env['TWILIO_ACCOUNT_SID'] ?? '';
export const TWILIO_AUTH_TOKEN = process.env['TWILIO_AUTH_TOKEN'] ?? '';
export const TWILIO_FROM = process.env['TWILIO_FROM'] ?? '';
export const TWILIO_WHATSAPP_FROM = process.env['TWILIO_WHATSAPP_FROM'] ?? '';
