import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

import { DrawResultOpsResponse } from '../../data-access/platform-ops-api.service';

interface SourceResultDialogData {
  readonly row: DrawResultOpsResponse;
}

interface SourcePickView {
  readonly label: string;
  readonly present: boolean;
  readonly gameCode: string;
  readonly status: string;
  readonly quality: string;
  readonly occurredAt: string;
  readonly main: readonly string[];
  readonly extras: readonly string[];
  readonly flags: readonly SourceFlagView[];
}

interface SourceFlagView {
  readonly label: string;
  readonly value: string;
}

@Component({
  selector: 'tch-source-result-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule],
  templateUrl: './source-result.dialog.html',
  styleUrls: ['./source-result.dialog.scss'],
})
export class SourceResultDialog {
  protected readonly data = inject<SourceResultDialogData>(MAT_DIALOG_DATA);
  protected readonly picks = [
    this.pickView('Pick 3', 'pick3'),
    this.pickView('Pick 4', 'pick4'),
  ];

  private pickView(label: string, key: 'pick3' | 'pick4'): SourcePickView {
    const root = asRecord(this.data.row.sourceResult);
    const pick = asRecord(root?.[key]);
    const flags = asRecord(pick?.['source_flags']);

    return {
      label,
      present: Boolean(pick?.['found']),
      gameCode: text(pick?.['game_code']),
      status: text(pick?.['status']),
      quality: text(pick?.['quality']),
      occurredAt: text(pick?.['occurred_at']),
      main: stringList(pick?.['main']),
      extras: stringList(pick?.['extras']),
      flags: flagsView(flags),
    };
  }
}

function asRecord(value: unknown): Record<string, unknown> | null {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

function text(value: unknown): string {
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean'
    ? String(value).trim()
    : '';
}

function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.map(text).filter(Boolean) : [];
}

function flagsView(flags: Record<string, unknown> | null): SourceFlagView[] {
  if (!flags) return [];

  const rows: SourceFlagView[] = [];
  add(rows, 'Origin', flags['origin']);
  add(rows, 'Hash', flags['sourceHash']);
  add(rows, 'URL', flags['url']);

  const metadata = asRecord(flags['metadata']);
  if (metadata) {
    for (const [key, value] of Object.entries(metadata)) {
      add(rows, key, value);
    }
  }

  return rows;
}

function add(rows: SourceFlagView[], label: string, value: unknown): void {
  const normalized = text(value);
  if (!normalized) return;
  rows.push({ label, value: normalized });
}
