import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

import { TchSectionError } from '@tch/ui/components';
import { AdminStatusPillComponent } from '@tch/ui/console';
import { ExecutionResponse } from '../../data-access/platform-ops-api.service';

@Component({
  selector: 'tch-execution-details-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, AdminStatusPillComponent, MatButtonModule, MatDialogModule, MatIconModule, TchSectionError],
  templateUrl: './execution-details.dialog.html',
  styleUrls: ['./execution-details.dialog.scss'],
})
export class ExecutionDetailsDialog {
  private readonly data = inject<{ execution: ExecutionResponse }>(MAT_DIALOG_DATA);

  readonly execution = this.data.execution;
  readonly payload = JSON.stringify(this.execution, null, 2);
  readonly copyFeedback = signal<{ title: string; message: string; severity: 'info' | 'error' } | null>(null);

  copy(): void {
    void navigator.clipboard.writeText(this.payload).then(
      () => this.copyFeedback.set({
        title: 'Détail copié',
        message: 'La référence technique est disponible dans le presse-papiers.',
        severity: 'info',
      }),
      () => this.copyFeedback.set({
        title: 'Copie impossible',
        message: 'Copiez manuellement les données affichées.',
        severity: 'error',
      }),
    );
  }

  statusTone(status: string): 'success' | 'danger' | 'warning' | 'neutral' | 'info' {
    return statusTone(status);
  }
}

function statusTone(status: string): 'success' | 'danger' | 'warning' | 'neutral' | 'info' {
  switch (status) {
    case 'COMPLETED':
    case 'SUCCESS':
      return 'success';
    case 'FAILED':
      return 'danger';
    case 'STARTED':
    case 'STARTING':
    case 'STOPPING':
      return 'warning';
    case 'UNKNOWN':
    case 'STOPPED':
    case 'ABANDONED':
      return 'neutral';
    default:
      return 'info';
  }
}
