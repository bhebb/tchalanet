import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import {
  ArchiveDomainPurgeDataset,
  ArchivePurgeResult,
  PlatformArchiveApi,
} from '../../../../data-access/platform-archive-api.service';
import { ArchiveRawRecordListComponent } from '../archive-raw-record-list/archive-raw-record-list.component';

@Component({
  selector: 'tch-archive-purge-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ArchiveRawRecordListComponent,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
    ReactiveFormsModule,
  ],
  templateUrl: './archive-purge-panel.component.html',
  styleUrls: ['./archive-purge-panel.component.scss'],
})
export class ArchivePurgePanelComponent {
  private readonly api = inject(PlatformArchiveApi);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly lastResult = signal<ArchivePurgeResult | null>(null);

  readonly form = this.fb.nonNullable.group({
    target: this.fb.nonNullable.control<'TICKETS' | ArchiveDomainPurgeDataset>('TICKETS', { validators: [Validators.required] }),
    tenantId: this.fb.control<string | null>(null),
    periodStart: this.fb.nonNullable.control('', { validators: [Validators.required] }),
    periodEnd: this.fb.nonNullable.control('', { validators: [Validators.required] }),
    batchSize: this.fb.nonNullable.control(5000, { validators: [Validators.required, Validators.min(1), Validators.max(100000)] }),
    reason: this.fb.nonNullable.control('', { validators: [Validators.required, Validators.minLength(10)] }),
  });

  runDryRun(): void {
    this.submit('DRY_RUN');
  }

  runDelete(): void {
    this.submit('DELETE');
  }

  resultRows(): Record<string, unknown>[] {
    const result = this.lastResult();
    return result ? [result as unknown as Record<string, unknown>] : [];
  }

  canDelete(): boolean {
    return this.lastResult()?.plan?.eligible === true && !this.loading();
  }

  private submit(mode: 'DRY_RUN' | 'DELETE'): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const common = {
      tenantId: value.tenantId || undefined,
      periodStart: value.periodStart,
      periodEnd: value.periodEnd,
      batchSize: value.batchSize,
      mode,
      reason: value.reason,
    };
    const request$ = value.target === 'TICKETS'
      ? this.api.purgeTickets(common)
      : this.api.purgeDomain({ ...common, dataset: value.target });

    this.loading.set(true);
    this.error.set(null);
    request$.subscribe({
      next: result => {
        this.lastResult.set(result);
        this.loading.set(false);
        this.snackBar.open(mode === 'DRY_RUN' ? 'Plan de purge calculé.' : 'Purge exécutée.', 'OK', { duration: 4000 });
      },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string; detail?: string } })?.error?.detail
          ?? (err as { error?: { title?: string } })?.error?.title
          ?? 'Purge refusée.');
        this.loading.set(false);
      },
    });
  }
}
