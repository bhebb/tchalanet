import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminStatusPillComponent } from '../../../../shared/admin-ui/admin-status-pill.component';
import {
  PlatformOpsApi,
  OpsLaunchResponse,
  GenerateDrawsRequest,
} from '../../data-access/platform-ops-api.service';

@Component({
  selector: 'tch-generate-draws-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
    AdminStatusPillComponent,
    TchSectionError,
  ],
  templateUrl: './generate-draws.dialog.html',
  styleUrls: ['./generate-draws.dialog.scss'],
})
export class GenerateDrawsDialog {
  readonly dialogRef = inject(MatDialogRef<GenerateDrawsDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly result = signal<OpsLaunchResponse | null>(null);
  readonly outcomeColumns = ['tenantId', 'ok', 'detail'];

  readonly form = this.fb.group({
    from: ['', Validators.required],
    to: ['', Validators.required],
    tenantCodes: [''],
    dryRun: [true],
    force: [false],
    reason: [''],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    if (!v.from || !v.to) return;
    const req: GenerateDrawsRequest = {
      from: v.from,
      to: v.to,
      tenantCodes: v.tenantCodes ? v.tenantCodes.split(',').map(s => s.trim()).filter(Boolean) : undefined,
      dryRun: v.dryRun ?? true,
      force: v.force ?? false,
      reason: v.reason || undefined,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.generateDraws(req, { suppressShellFeedback: true }).subscribe({
      next: (res) => { this.submitting.set(false); this.result.set(res); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(this.errorViewModel(err, 'platform.ops.draws.generate'));
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
