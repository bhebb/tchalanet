import Fastify from 'fastify';

import {
  BREVO_API_KEY,
  EDGE_HMAC_SECRET,
  EMAIL_ENABLED,
  EMAIL_FROM_ADDRESS,
  EMAIL_FROM_NAME,
  SLACK_ENABLED,
  SLACK_WEBHOOKS,
  SMS_ENABLED,
  TWILIO_ACCOUNT_SID,
  TWILIO_AUTH_TOKEN,
  TWILIO_FROM,
} from './config/env.js';
import { TicketDeliveryService } from './modules/delivery/application/ticket-delivery.service.js';
import { deliveryRoutes } from './modules/delivery/http/delivery.routes.js';
import { healthRoutes } from './modules/health/health.routes.js';
import { BrevoEmailMessageSender } from './modules/messages/adapters/email/brevo-email-message.sender.js';
import { SlackMessageSender } from './modules/messages/adapters/slack/slack-message.sender.js';
import { TwilioSmsMessageSender } from './modules/messages/adapters/sms/twilio-sms-message.sender.js';
import { SendMessageService } from './modules/messages/application/send-message.service.js';
import { messageRoutes } from './modules/messages/http/message.routes.js';
import { pingRoutes } from './modules/ping/ping.routes.js';
import { errorHandlerPlugin } from './plugins/error-handler.plugin.js';

export function buildApp() {
  const app = Fastify({ logger: true });

  const messageService = new SendMessageService([
    new SlackMessageSender(SLACK_ENABLED, SLACK_WEBHOOKS),
    new BrevoEmailMessageSender(EMAIL_ENABLED, BREVO_API_KEY, EMAIL_FROM_NAME, EMAIL_FROM_ADDRESS),
    new TwilioSmsMessageSender(SMS_ENABLED, TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM),
  ]);

  const deliveryService = new TicketDeliveryService([
    new SlackMessageSender(SLACK_ENABLED, SLACK_WEBHOOKS),
    new BrevoEmailMessageSender(EMAIL_ENABLED, BREVO_API_KEY, EMAIL_FROM_NAME, EMAIL_FROM_ADDRESS),
    new TwilioSmsMessageSender(SMS_ENABLED, TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM),
  ]);

  app.register(errorHandlerPlugin);
  app.register(pingRoutes);
  app.register(healthRoutes);
  app.register(messageRoutes(messageService, { secret: EDGE_HMAC_SECRET }));
  app.register(deliveryRoutes(deliveryService));

  return app;
}
