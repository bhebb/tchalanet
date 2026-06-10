import { ShellFeedbackItem } from './shell-feedback.model';

export function buildCopyText(item: Pick<ShellFeedbackItem, 'title' | 'message' | 'status' | 'traceId' | 'source'>): string {
  const lines: string[] = ['Erreur Tchalanet'];
  if (item.status !== undefined) lines.push(`Statut: ${item.status}`);
  lines.push(`Titre: ${item.title}`);
  lines.push(`Message: ${item.message}`);
  if (item.traceId) lines.push(`Trace ID: ${item.traceId}`);
  if (item.source) lines.push(`Chemin: ${item.source}`);
  lines.push(`Date: ${new Date().toISOString()}`);
  return lines.join('\n');
}

export function copyToClipboard(text: string): void {
  void navigator.clipboard.writeText(text);
}
