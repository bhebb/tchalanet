import axios from 'axios';
import { EDGE_CONFIG } from '../config/env';

interface MailgunSendParams {
  to: string;
  subject: string;
  html: string;
}

export async function sendEmailWithMailgun(params: MailgunSendParams): Promise<string> {
  if (!EDGE_CONFIG.mailgun.apiKey || !EDGE_CONFIG.mailgun.domain) {
    console.log('[mailgun:stub] would send email to %s with subject "%s"', params.to, params.subject);
    return 'mailgun-mock-id';
  }

  const url = 'https://api.mailgun.net/v3/' + EDGE_CONFIG.mailgun.domain + '/messages';
  const auth = 'Basic ' + Buffer.from('api:' + EDGE_CONFIG.mailgun.apiKey).toString('base64');

  await axios.post(
    url,
    new URLSearchParams({
      from: EDGE_CONFIG.mailgun.from,
      to: params.to,
      subject: params.subject,
      html: params.html
    }),
    { headers: { Authorization: auth } }
  );

  return 'mailgun-real-id';
}
