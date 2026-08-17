import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  untracked,
} from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { mapHttpErrorToProblemDetail, webAppErrorsFromProblemDetailFields } from '@tch/api';
import { RuntimeSettingsStore } from '@tch/shared-config';
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
  TenantParametersApiService,
  TenantInternalConfig,
} from '../../data-access/tenant-parameters-api.service';

const PAPER_SIZES = ['RECEIPT_58MM', 'RECEIPT_80MM', 'A4'] as const;

const RECEIPT_FIELD_TARGETS: Record<string, string> = {
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
  selector: 'tch-admin-receipt-config-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
    TchFieldError,
    TchNotice,
  ],
  templateUrl: './admin-receipt-config.page.html',
  styleUrls: ['./admin-receipt-config.page.scss'],
})
export class AdminReceiptConfigPage {
  private readonly api = inject(TenantParametersApiService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly runtimeSettings = inject(RuntimeSettingsStore);
  private readonly translate = inject(TranslateService);

  readonly fromSetup = this.route.snapshot.queryParamMap.get('from') === 'setup';
  readonly backRoute = this.fromSetup ? '/app/admin/setup' : '/app/admin/company/settings';
  readonly backLabel = this.fromSetup ? 'admin.setup.backToSetup' : 'admin.settings.title';

  readonly paperSizes = PAPER_SIZES;
  readonly config = this.api.tenantConfigResource();
  readonly configError = resourceErrorVm(this.config, 'admin.setup.config');
  readonly configIsEmpty = () => false;
  readonly readiness = this.api.readinessResource();

  readonly receiptTemplateLabelKey = computed(() => {
    const key = this.config.value()?.document?.receipt?.defaultTemplateKey ?? null;
    if (key === 'sales.ticket.receipt.v1') return 'admin.settings.config.receipt.templateNames.ticketReceiptV1';
    return key;
  });

  private lastConfig: TenantInternalConfig = {};

  readonly receiptForm = this.fb.group({
    enabled: new FormControl<boolean>(true, { nonNullable: true }),
    headerMessage: new FormControl<string>('', { nonNullable: true }),
    footerMessage: new FormControl<string>(
      this.translate.instant('admin.settings.config.receipt.footerDefault'),
      { nonNullable: true },
    ),
    defaultPaperSize: new FormControl<string>('RECEIPT_80MM', { nonNullable: true }),
    showQrCode: new FormControl<boolean>(true, { nonNullable: true }),
  });

  readonly saveReceipt = tchMutation<NonNullable<TenantInternalConfig['document']>, void>({
    run: document =>
      this.api.updateSettingsSection('document', document, { suppressShellFeedback: true }),
    source: 'admin.setup.receipt',
    onSuccess: (_result, input) => {
      this.rememberConfig({ ...this.lastConfig, document: input });
      this.receiptForm.markAsPristine();
      this.config.reload();
    },
    onError: err => this.applyFieldErrors(err),
  });

  constructor() {
    effect(() => {
      if (!this.config.hasValue()) return;
      const cfg = this.config.value() ?? {};
      untracked(() => {
        this.rememberConfig(cfg);
        if (!this.receiptForm.dirty) this.patchForm(cfg);
      });
    });
  }

  sectionReadiness(section: string) {
    return this.readiness.value()?.sections.find(item => item.section === section) ?? null;
  }

  submitReceipt(): void {
    clearServerFieldErrors(this.receiptForm);
    this.saveReceipt.clearFeedback();
    const v = this.receiptForm.getRawValue();
    this.saveReceipt.execute({
      ...this.lastConfig.document,
      receipt: {
        ...this.lastConfig.document?.receipt,
        enabled: v.enabled,
        headerMessage: v.headerMessage || null,
        footerMessage: v.footerMessage || null,
        defaultPaperSize: v.defaultPaperSize || null,
        showQrCode: v.showQrCode,
      },
    });
  }

  private patchForm(cfg: TenantInternalConfig): void {
    const r = cfg.document?.receipt;
    if (!r) return;
    this.receiptForm.patchValue({
      enabled: r.enabled ?? true,
      headerMessage: r.headerMessage ?? '',
      footerMessage:
        r.footerMessage ?? this.translate.instant('admin.settings.config.receipt.footerDefault'),
      defaultPaperSize: r.defaultPaperSize ?? 'RECEIPT_80MM',
      showQrCode: r.showQrCode ?? true,
    });
  }

  private rememberConfig(config: TenantInternalConfig): void {
    this.lastConfig = config;
    this.runtimeSettings.applyTenantDashboardSettings({
      supportedLanguages: config.locale?.supportedLanguages,
      fallbackLanguage: config.locale?.fallbackLanguage,
      settings: config,
    });
  }

  private applyFieldErrors(err: unknown): boolean {
    const problem = mapHttpErrorToProblemDetail(err);
    const fieldErrors = withResolvedErrorCopies(
      webAppErrorsFromProblemDetailFields(problem, 'admin.setup.receipt'),
      key => this.translate.instant(key),
    );
    const remaining = applyServerFieldErrors(this.receiptForm, fieldErrors, RECEIPT_FIELD_TARGETS);
    return fieldErrors.length > 0 && remaining.length === 0;
  }
}
