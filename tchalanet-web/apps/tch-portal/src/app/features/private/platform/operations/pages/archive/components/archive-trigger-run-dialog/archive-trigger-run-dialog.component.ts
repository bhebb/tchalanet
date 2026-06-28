import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import {
  ArchiveRunView,
  PlatformArchiveApi,
  TriggerArchiveRunRequest,
} from '../../../../data-access/platform-archive-api.service';

@Component({
  selector: 'tch-archive-trigger-run-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './archive-trigger-run-dialog.component.html',
  styleUrls: ['./archive-trigger-run-dialog.component.scss'],
})
export class ArchiveTriggerRunDialogComponent {
  private readonly api = inject(PlatformArchiveApi);
  private readonly ref = inject(MatDialogRef<ArchiveTriggerRunDialogComponent>);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    strategy: ['', Validators.required],
    periodStart: ['', Validators.required],
    periodEnd: ['', Validators.required],
    reason: ['', [Validators.required, Validators.minLength(10)]],
  });

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: TriggerArchiveRunRequest = {
      strategy: v.strategy.toUpperCase(),
      periodStart: v.periodStart,
      periodEnd: v.periodEnd,
      reason: v.reason,
    };
    this.api.triggerRun(req).subscribe({
      next: (run: ArchiveRunView) => this.ref.close(run),
      error: (err: unknown) => {
        this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' });
      },
    });
  }
}
