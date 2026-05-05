import Fastify from 'fastify';

import {
  BREVO_API_KEY,
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
import { BrevoEmailNotificationSender } from './modules/notifications/adapters/email/brevo-email-notification.sender.js';
import { SlackNotificationSender } from './modules/notifications/adapters/slack/slack-notification.sender.js';
import { TwilioSmsNotificationSender } from './modules/notifications/adapters/sms/twilio-sms-notification.sender.js';
import { SendNotificationService } from './modules/notifications/application/send-notification.service.js';
import { notificationRoutes } from './modules/notifications/http/notification.routes.js';
import { pingRoutes } from './modules/ping/ping.routes.js';
import { errorHandlerPlugin } from './plugins/error-handler.plugin.js';

export function buildApp() {
  const app = Fastify({ logger: true });

  const notificationService = new SendNotificationService([
    new SlackNotificationSender(SLACK_ENABLED, SLACK_WEBHOOKS),
    new BrevoEmailNotificationSender(
      EMAIL_ENABLED,
      BREVO_API_KEY,
      EMAIL_FROM_NAME,
      EMAIL_FROM_ADDRESS,
    ),
    new TwilioSmsNotificationSender(
      SMS_ENABLED,
      TWILIO_ACCOUNT_SID,
      TWILIO_AUTH_TOKEN,
      TWILIO_FROM,
    ),
  ]);

  const deliveryService = new TicketDeliveryService([
    new SlackNotificationSender(SLACK_ENABLED, SLACK_WEBHOOKS),
    new BrevoEmailNotificationSender(
      EMAIL_ENABLED,
      BREVO_API_KEY,
      EMAIL_FROM_NAME,
      EMAIL_FROM_ADDRESS,
    ),
    new TwilioSmsNotificationSender(
      SMS_ENABLED,
      TWILIO_ACCOUNT_SID,
      TWILIO_AUTH_TOKEN,
      TWILIO_FROM,
    ),
  ]);

  app.register(errorHandlerPlugin);
  app.register(pingRoutes);
  app.register(healthRoutes);
  app.register(notificationRoutes(notificationService));
  app.register(deliveryRoutes(deliveryService));

  return app;
}
