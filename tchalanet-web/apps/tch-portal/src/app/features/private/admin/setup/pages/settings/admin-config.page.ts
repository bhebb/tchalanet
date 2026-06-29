import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  FormControl,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail, webAppErrorsFromProblemDetailFields } from '@tch/api';
import { TchLoading, TchErrorPanel, TchFieldError, TchNotice } from '@tch/ui/components';
import {
  applyServerFieldErrors,
  clearServerFieldErrors,
  ErrorViewModel,
  toErrorViewModel,
  withResolvedErrorCopies,
} from '@tch/web/errors';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import {
  TenantConfigApiService,
  TenantInternalConfig,
} from '../../data-access/tenant-config-api.service';

type PageState = 'loading' | 'ready' | 'error';
type SaveState = 'idle' | 'saving';

const SUPPORTED_LANGUAGES = [
  { code: 'fr', label: 'Français' },
  { code: 'en', label: 'English' },
  { code: 'ht', label: 'Kreyòl Ayisyen' },
] as const;

const PAPER_SIZES = ['THERMAL_58', 'THERMAL_80', 'A4'] as const;

@Component({
  selector: 'tch-admin-config-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchLoading,
    TchErrorPanel,
    TchFieldError,
    TchNotice,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTabsModule,
  ],
  templateUrl: './admin-config.page.html',
  styleUrls: ['./admin-config.page.scss'],
})
export class AdminConfigPage implements OnInit {
  private readonly api = inject(TenantConfigApiService);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly saveState = signal<SaveState>('idle');
  readonly receiptSaveState = signal<SaveState>('idle');
  readonly localeFormError = signal<string | null>(null);
  readonly receiptFormError = signal<string | null>(null);
  readonly localeNotice = signal<string | null>(null);
  readonly receiptNotice = signal<string | null>(null);

  readonly languages = SUPPORTED_LANGUAGES;
  readonly paperSizes = PAPER_SIZES;

  readonly localeForm = this.fb.group({
    defaultLanguage: new FormControl<string>('fr', { nonNullable: true, validators: [Validators.required] }),
    defaultLocale: new FormControl<string>('fr-HT', { nonNullable: true }),
    fallbackLanguage: new FormControl<string>('fr', { nonNullable: true }),
  });

  readonly receiptForm = this.fb.group({
    enabled: new FormControl<boolean>(true, { nonNullable: true }),
    displayName: new FormControl<string>('', { nonNullable: true }),
    headerMessage: new FormControl<string>('', { nonNullable: true }),
    footerMessage: new FormControl<string>('', { nonNullable: true }),
    defaultPaperSize: new FormControl<string>('THERMAL_80', { nonNullable: true }),
    showQrCode: new FormControl<boolean>(true, { nonNullable: true }),
    showSellerName: new FormControl<boolean>(true, { nonNullable: true }),
    showOutletName: new FormControl<boolean>(false, { nonNullable: true }),
    showPotentialPayout: new FormControl<boolean>(false, { nonNullable: true }),
  });

  private config: TenantInternalConfig | null = null;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.localeNotice.set(null);
    this.receiptNotice.set(null);

    this.api.getTenantConfig({ suppressShellFeedback: true }).subscribe({
      next: cfg => {
        this.config = cfg;
        this.patchLocale(cfg);
        this.patchReceipt(cfg);
        this.pageState.set('ready');
      },
      error: (err: unknown) => {
        this.pageError.set(this.errorViewModel(err, 'admin.setup.config', 'page'));
        this.pageState.set('error');
      },
    });
  }

  saveLocale(): void {
    clearServerFieldErrors(this.localeForm);
    this.localeFormError.set(null);
    this.localeNotice.set(null);
    if (this.localeForm.invalid) {
      this.localeForm.markAllAsTouched();
      return;
    }
    this.saveState.set('saving');
    const v = this.localeForm.getRawValue();
    const updated: TenantInternalConfig = {
      ...this.config,
      locale: {
        defaultLanguage: v.defaultLanguage,
        defaultLocale: v.defaultLocale || null,
        fallbackLanguage: v.fallbackLanguage || null,
      },
    };
    this.api.updateInternalSettings(updated, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.config = updated;
        this.saveState.set('idle');
        this.localeNotice.set('Paramètres de langue enregistrés.');
      },
      error: (err: unknown) => {
        this.handleLocaleSubmitError(err);
        this.saveState.set('idle');
      },
    });
  }

  saveReceipt(): void {
    clearServerFieldErrors(this.receiptForm);
    this.receiptFormError.set(null);
    this.receiptNotice.set(null);
    this.receiptSaveState.set('saving');
    const v = this.receiptForm.getRawValue();
    const updated: TenantInternalConfig = {
      ...this.config,
      document: {
        receipt: {
          enabled: v.enabled,
          displayName: v.displayName || null,
          headerMessage: v.headerMessage || null,
          footerMessage: v.footerMessage || null,
          defaultPaperSize: v.defaultPaperSize || null,
          showQrCode: v.showQrCode,
          showSellerName: v.showSellerName,
          showOutletName: v.showOutletName,
          showPotentialPayout: v.showPotentialPayout,
        },
      },
    };
    this.api.updateInternalSettings(updated, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.config = updated;
        this.receiptSaveState.set('idle');
        this.receiptNotice.set('Configuration du reçu enregistrée.');
      },
      error: (err: unknown) => {
        this.handleReceiptSubmitError(err);
        this.receiptSaveState.set('idle');
      },
    });
  }

  private patchLocale(cfg: TenantInternalConfig): void {
    const loc = cfg.locale;
    if (!loc) return;
    this.localeForm.patchValue({
      defaultLanguage: loc.defaultLanguage ?? 'fr',
      defaultLocale: loc.defaultLocale ?? 'fr-HT',
      fallbackLanguage: loc.fallbackLanguage ?? 'fr',
    });
  }

  private patchReceipt(cfg: TenantInternalConfig): void {
    const r = cfg.document?.receipt;
    if (!r) return;
    this.receiptForm.patchValue({
      enabled: r.enabled ?? true,
      displayName: r.displayName ?? '',
      headerMessage: r.headerMessage ?? '',
      footerMessage: r.footerMessage ?? '',
      defaultPaperSize: r.defaultPaperSize ?? 'THERMAL_80',
      showQrCode: r.showQrCode ?? true,
      showSellerName: r.showSellerName ?? true,
      showOutletName: r.showOutletName ?? false,
      showPotentialPayout: r.showPotentialPayout ?? false,
    });
  }

  private handleLocaleSubmitError(err: unknown): void {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const fieldErrors = withResolvedErrorCopies(
        webAppErrorsFromProblemDetailFields(problem, 'admin.setup.locale'),
        key => this.translate.instant(key),
      );
      const remaining = applyServerFieldErrors(this.localeForm, fieldErrors, {
        'locale.defaultLanguage': 'defaultLanguage',
        'admin.setup.locale.defaultLanguage': 'defaultLanguage',
        'tenant.locale.defaultLanguage': 'defaultLanguage',
        'locale.defaultLocale': 'defaultLocale',
        'admin.setup.locale.defaultLocale': 'defaultLocale',
        'tenant.locale.defaultLocale': 'defaultLocale',
        'locale.fallbackLanguage': 'fallbackLanguage',
        'admin.setup.locale.fallbackLanguage': 'fallbackLanguage',
        'tenant.locale.fallbackLanguage': 'fallbackLanguage',
      });

      if (fieldErrors.length && !remaining.length) {
        this.localeFormError.set(null);
        return;
      }
    }

    this.localeFormError.set(this.formErrorMessage(problem, 'admin.setup.locale'));
  }

  private handleReceiptSubmitError(err: unknown): void {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const fieldErrors = withResolvedErrorCopies(
        webAppErrorsFromProblemDetailFields(problem, 'admin.setup.receipt'),
        key => this.translate.instant(key),
      );
      const remaining = applyServerFieldErrors(this.receiptForm, fieldErrors, {
        'document.receipt.displayName': 'displayName',
        'admin.setup.receipt.displayName': 'displayName',
        'receipt.displayName': 'displayName',
        'document.receipt.headerMessage': 'headerMessage',
        'admin.setup.receipt.headerMessage': 'headerMessage',
        'receipt.headerMessage': 'headerMessage',
        'document.receipt.footerMessage': 'footerMessage',
        'admin.setup.receipt.footerMessage': 'footerMessage',
        'receipt.footerMessage': 'footerMessage',
        'document.receipt.defaultPaperSize': 'defaultPaperSize',
        'admin.setup.receipt.defaultPaperSize': 'defaultPaperSize',
        'receipt.defaultPaperSize': 'defaultPaperSize',
      });

      if (fieldErrors.length && !remaining.length) {
        this.receiptFormError.set(null);
        return;
      }
    }

    this.receiptFormError.set(this.formErrorMessage(problem, 'admin.setup.receipt'));
  }

  private formErrorMessage(problem: ProblemDetail | undefined, source: string): string {
    if (!problem) return this.errorFallback().message;

    const normalized = webAppErrorFromProblemDetail(problem, source, 'section');
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy).message;
  }

  private errorViewModel(
    err: unknown,
    source: string,
    surface: 'page' | 'section',
  ): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, surface);
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return this.errorFallback();
  }

  private errorFallback(): ErrorViewModel {
    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}
