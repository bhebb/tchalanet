import dotenv from 'dotenv';

dotenv.config();

export const EDGE_CONFIG = {
  port: process.env.PORT ? parseInt(process.env.PORT, 10) : 4001,
  mailgun: {
    apiKey: process.env.MAILGUN_API_KEY || '',
    domain: process.env.MAILGUN_DOMAIN || '',
    from: process.env.MAILGUN_FROM || 'Tchalanet <no-reply@tchalanet.com>'
  },
  bird: {
    accessKey: process.env.BIRD_ACCESS_KEY || '',
    workspaceId: process.env.BIRD_WORKSPACE_ID || '',
    smsChannelId: process.env.BIRD_SMS_CHANNEL_ID || '',
    whatsappChannelId: process.env.BIRD_WHATSAPP_CHANNEL_ID || ''
  }
};
