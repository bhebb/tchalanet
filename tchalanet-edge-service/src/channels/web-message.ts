interface WebMessageParams {
  tenantId: string;
  payload: string;
}

export async function sendWebMessage(params: WebMessageParams): Promise<string> {
  console.log('[web-message] tenant=%s payload=%s', params.tenantId, params.payload);
  return 'web-message-id';
}
