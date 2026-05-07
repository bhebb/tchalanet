import type { MessageRecipient, SendMessageRequest } from '../domain/message.js';

export interface MessageSender {
  supports(recipient: MessageRecipient): boolean;
  send(message: SendMessageRequest, recipient: MessageRecipient): Promise<void>;
}
