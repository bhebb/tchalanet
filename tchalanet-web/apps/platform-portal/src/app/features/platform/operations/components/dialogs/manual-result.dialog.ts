import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { PlatformOpsApi, RecordManualDrawResultRequest } from '../../data-access/platform-ops-api.service';
import {
  CatalogResultSlotView,
  PlatformCatalogApi,
} from '../../../catalog/data-access/platform-catalog-api.service';
import { haitiLotGameMappings } from '../../../../../shared/results/haiti-lot-game-mapping';
import { resultSlotLabel } from '../../../../../shared/results/result-slot-label';

@Component({
  selector: 'tch-manual-result-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TchSectionError,
  ],
  templateUrl: './manual-result.dialog.html',
  styleUrls: ['./manual-result.dialog.scss'],
})
export class ManualResultDialog implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<ManualResultDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly catalogApi = inject(PlatformCatalogApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly today = todayIsoDate();
  readonly slots = signal<CatalogResultSlotView[]>([]);
  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);

  readonly form = this.fb.group({
    slotKey: ['', Validators.required],
    drawDate: [this.today, [Validators.required, notFutureDateValidator]],
    lot1: ['', [Validators.required, Validators.pattern(/^\d{3}$/)]],
    lot2: ['', [Validators.required, Validators.pattern(/^\d{2}$/)]],
    lot3: ['', [Validators.required, Validators.pattern(/^\d{2}$/)]],
    reason: ['', Validators.required],
    notes: [''],
    force: [false],
  });

  ngOnInit(): void {
    this.catalogApi.listResultSlots().subscribe({
      next: slots => {
        const sorted = [...slots].sort((a, b) => a.slotKey.localeCompare(b.slotKey));
        this.slots.set(sorted);
        if (!this.form.controls.slotKey.value && sorted.length === 1) {
          this.form.patchValue({ slotKey: sorted[0].slotKey });
        }
      },
      error: err => {
        this.error.set(this.errorViewModel(err, 'platform.ops.drawResults.manual.slots'));
      },
    });
  }

  slotLabel(slot: CatalogResultSlotView): string {
    return resultSlotLabel(slot);
  }

  lotMappings() {
    const slotKey = this.form.controls.slotKey.value;
    const slot = this.slots().find(item => item.slotKey === slotKey);
    return haitiLotGameMappings(slot ?? { slotKey });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    if (!v.slotKey || !v.drawDate || !v.lot1 || !v.lot2 || !v.lot3 || !v.reason) return;
    const req: RecordManualDrawResultRequest = {
      slotKey: v.slotKey,
      drawDate: v.drawDate,
      recordedBy: 'platform-ops',
      lot1: v.lot1,
      lot2: v.lot2,
      lot3: v.lot3,
      reason: v.reason,
      notes: v.notes || undefined,
      force: v.force ?? false,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.manualDrawResult(req, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.dialogRef.close(true);
      },
      error: err => {
        this.submitting.set(false);
        this.error.set(this.errorViewModel(err, 'platform.ops.drawResults.manual'));
      },
    });
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}

function todayIsoDate(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function notFutureDateValidator(control: AbstractControl): { futureDate: true } | null {
  const value = control.value;
  return typeof value === 'string' && value > todayIsoDate() ? { futureDate: true } : null;
}
