import { ShellFeedbackItem } from './shell-feedback.model';

export function buildCopyText(
  item: Pick<ShellFeedbackItem, 'requestId' | 'traceId' | 'spanId' | 'errorId' | 'source' | 'title' | 'message' | 'status'>,
): string {
  const parts: string[] = [];
  if (item.requestId) parts.push(`requestId=${item.requestId}`);
  if (item.traceId)   parts.push(`traceId=${item.traceId}`);
  if (item.spanId)    parts.push(`spanId=${item.spanId}`);
  if (item.errorId)   parts.push(`errorId=${item.errorId}`);
  if (item.source)    parts.push(`route=${item.source}`);
  parts.push(`time=${new Date().toISOString()}`);
  if (item.status !== undefined) parts.push(`status=${item.status}`);
  parts.push(`title=${item.title}`);
  if (item.message && item.message !== item.title) parts.push(`message=${item.message}`);
  return parts.join(' ');
}

export function copyToClipboard(text: string): void {
  void navigator.clipboard.writeText(text);
}
