import axios from 'axios';
import { EDGE_CONFIG } from '../config/env';

interface BirdSmsParams {
  to: string;
  text: string;
}

export async function sendSmsWithBird(params: BirdSmsParams): Promise<string> {
  if (!EDGE_CONFIG.bird.accessKey || !EDGE_CONFIG.bird.smsChannelId || !EDGE_CONFIG.bird.workspaceId) {
    console.log('[bird:stub] would send SMS to %s: %s', params.to, params.text);
    return 'bird-mock-id';
  }

  await axios.post(
    'https://api.bird.com/channels/v1/messages',
    {
      workspaceId: EDGE_CONFIG.bird.workspaceId,
      to: { msisdn: params.to },
      body: params.text,
      channelId: EDGE_CONFIG.bird.smsChannelId
    },
    {
      headers: {
        Authorization: 'AccessKey ' + EDGE_CONFIG.bird.accessKey,
        'Content-Type': 'application/json'
      }
    }
  );

  return 'bird-real-id';
}
