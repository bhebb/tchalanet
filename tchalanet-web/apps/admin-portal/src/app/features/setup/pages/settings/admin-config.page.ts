import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorsFromProblemDetailFields } from '@tch/api';
import { TchFieldError, TchNotice } from '@tch/ui/components';
import {
  applyServerFieldErrors,
  clearServerFieldErrors,
  withResolvedErrorCopies,
} from '@tch/web/errors';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
  tchMutation,
} from '@tch/web/async';
import { AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import {
  TenantConfigApiService,
  TenantInternalConfig,
} from '../../data-access/tenant-config-api.service';

const SUPPORTED_LANGUAGES = [
  { code: 'fr', label: 'Français' },
  { code: 'en', label: 'English' },
  { code: 'ht', label: 'Kreyòl Ayisyen' },
] as const;

const PAPER_SIZES = ['THERMAL_58', 'THERMAL_80', 'A4'] as const;

const LOCALE_FIELD_TARGETS: Record<string, string> = {
  'locale.defaultLanguage': 'defaultLanguage',
  'admin.setup.locale.defaultLanguage': 'defaultLanguage',
  'tenant.locale.defaultLanguage': 'defaultLanguage',
  'locale.defaultLocale': 'defaultLocale',
  'admin.setup.locale.defaultLocale': 'defaultLocale',
  'tenant.locale.defaultLocale': 'defaultLocale',
  'locale.fallbackLanguage': 'fallbackLanguage',
  'admin.setup.locale.fallbackLanguage': 'fallbackLanguage',
  'tenant.locale.fallbackLanguage': 'fallbackLanguage',
};

const RECEIPT_FIELD_TARGETS: Record<string, string> = {
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
};

@Component({
  selector: 'tch-admin-config-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
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
export class AdminConfigPage {
  private readonly api = inject(TenantConfigApiService);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly languages = SUPPORTED_LANGUAGES;
  readonly paperSizes = PAPER_SIZES;

  readonly config = this.api.tenantConfigResource();
  readonly configError = resourceErrorVm(this.config, 'admin.setup.config');

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

  readonly saveLocale = tchMutation<TenantInternalConfig, void>({
    run: updated => this.api.updateInternalSettings(updated, { suppressShellFeedback: true }),
    source: 'admin.setup.locale',
    onSuccess: (_result, input) => {
      // maj immédiate : une 2e sauvegarde avant le retour du reload ne doit pas
      // repartir d'une config stale et écraser la section qui vient d'être enregistrée
      this.lastConfig = input;
      this.localeForm.markAsPristine();
      this.config.reload();
    },
    onError: err => this.applyFieldErrors(err, this.localeForm, 'admin.setup.locale', LOCALE_FIELD_TARGETS),
  });

  readonly saveReceipt = tchMutation<TenantInternalConfig, void>({
    run: updated => this.api.updateInternalSettings(updated, { suppressShellFeedback: true }),
    source: 'admin.setup.receipt',
    onSuccess: (_result, input) => {
      this.lastConfig = input;
      this.receiptForm.markAsPristine();
      this.config.reload();
    },
    onError: err => this.applyFieldErrors(err, this.receiptForm, 'admin.setup.receipt', RECEIPT_FIELD_TARGETS),
  });

  /** Dernière config connue — base des merges de sauvegarde (value() lance en état error). */
  private lastConfig: TenantInternalConfig = {};

  constructor() {
    // La valeur du resource alimente les formulaires — sans écraser une saisie en cours.
    effect(() => {
      if (!this.config.hasValue()) return;
      const cfg = this.config.value();
      if (!cfg) return;
      this.lastConfig = cfg;
      if (!this.localeForm.dirty) this.patchLocale(cfg);
      if (!this.receiptForm.dirty) this.patchReceipt(cfg);
    });
  }

  submitLocale(): void {
    clearServerFieldErrors(this.localeForm);
    this.saveLocale.clearFeedback();
    if (this.localeForm.invalid) {
      this.localeForm.markAllAsTouched();
      return;
    }
    const v = this.localeForm.getRawValue();
    this.saveLocale.execute({
      ...this.lastConfig,
      locale: {
        defaultLanguage: v.defaultLanguage,
        defaultLocale: v.defaultLocale || null,
        fallbackLanguage: v.fallbackLanguage || null,
      },
    });
  }

  submitReceipt(): void {
    clearServerFieldErrors(this.receiptForm);
    this.saveReceipt.clearFeedback();
    const v = this.receiptForm.getRawValue();
    this.saveReceipt.execute({
      ...this.lastConfig,
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

  /** Applique les erreurs serveur par champ ; `true` = tout est géré, pas de feedback générique. */
  private applyFieldErrors(
    err: unknown,
    form: typeof this.localeForm | typeof this.receiptForm,
    source: string,
    targets: Record<string, string>,
  ): boolean {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (!problem) return false;

    const fieldErrors = withResolvedErrorCopies(
      webAppErrorsFromProblemDetailFields(problem, source),
      key => this.translate.instant(key),
    );
    const remaining = applyServerFieldErrors(form, fieldErrors, targets);
    return fieldErrors.length > 0 && remaining.length === 0;
  }
}
