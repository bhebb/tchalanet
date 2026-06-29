import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail, webAppErrorsFromProblemDetailFields } from '@tch/api';

import { TchErrorPanel, TchFieldError } from '@tch/ui/components';
import {
  applyServerFieldErrors,
  clearServerFieldErrors,
  ErrorViewModel,
  toErrorViewModel,
  withResolvedErrorCopies,
} from '@tch/web/errors';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { AdminSectionCardComponent } from '../../../../../shared/admin-ui/admin-section-card.component';
import { CreateSellerTerminalRequest, SellerTerminalApi } from '../../../../seller-terminal-api.service';
import { SELLER_TERMINAL_CREATE_FIELD_TARGETS } from '../../../seller-terminal-error-targets';

@Component({
  selector: 'tch-create-seller-terminal-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    AdminSectionCardComponent,
    TchErrorPanel,
    TchFieldError,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './create-seller-terminal.dialog.html',
  styleUrls: ['./create-seller-terminal.dialog.scss'],
})
export class CreateSellerTerminalDialog implements OnInit {
  private readonly api = inject(SellerTerminalApi);
  private readonly dialogRef = inject(MatDialogRef<CreateSellerTerminalDialog>);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);

  readonly form = this.fb.group({
    terminalCode: ['', [Validators.required, Validators.maxLength(64)]],
    displayName: ['', [Validators.required, Validators.maxLength(180)]],
    firstName: [''],
    lastName: [''],
    email: ['', [Validators.email, Validators.maxLength(254)]],
    phoneNumber: [''],
    commissionRate: [null as number | null, [Validators.min(0), Validators.max(100)]],
    initialPin: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  ngOnInit(): void {
    this.api.getCommissionOverview().subscribe({
      next: overview => {
        if (overview.tenantDefaultRate != null && this.form.controls.commissionRate.value == null) {
          this.form.controls.commissionRate.setValue(overview.tenantDefaultRate);
        }
      },
      error: () => { /* Keep commission optional when the overview is unavailable. */ },
    });
  }

  submit(): void {
    if (this.saving()) return;
    clearServerFieldErrors(this.form);
    this.error.set(null);
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const v = this.form.getRawValue();
    const req: CreateSellerTerminalRequest = {
      terminalCode: v.terminalCode ?? '',
      displayName: v.displayName ?? '',
      firstName: v.firstName || null,
      lastName: v.lastName || null,
      email: v.email || null,
      phoneNumber: v.phoneNumber || null,
      commissionRate: v.commissionRate ?? null,
      initialPin: v.initialPin ?? '',
    };

    this.saving.set(true);
    this.api.create(req, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogRef.close({ reload: true });
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.handleCreateError(err);
      },
    });
  }

  serverFieldMessage(control: AbstractControl | null): string {
    const server = control?.errors?.['server'];
    return typeof server === 'object' &&
      server !== null &&
      'message' in server &&
      typeof (server as { message?: unknown }).message === 'string'
      ? (server as { message: string }).message
      : '';
  }

  private handleCreateError(err: unknown): void {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const fieldErrors = withResolvedErrorCopies(
        webAppErrorsFromProblemDetailFields(problem, 'admin.sellerTerminal.create'),
        key => this.translate.instant(key),
      );
      const remaining = applyServerFieldErrors(this.form, fieldErrors, SELLER_TERMINAL_CREATE_FIELD_TARGETS);

      if (fieldErrors.length && !remaining.length) {
        this.error.set(null);
        return;
      }

      const normalized = webAppErrorFromProblemDetail(problem, 'admin.sellerTerminal.create', 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      this.error.set(toErrorViewModel(normalized, copy));
      return;
    }

    this.error.set({
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    });
  }
}
