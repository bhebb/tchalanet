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
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { TchLoading, TchErrorPanel, TchNotice } from '@tch/ui/components';
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
  private readonly snackBar = inject(MatSnackBar);

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<string | null>(null);
  readonly saveState = signal<SaveState>('idle');
  readonly receiptSaveState = signal<SaveState>('idle');

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

    this.api.getTenantConfig().subscribe({
      next: cfg => {
        this.config = cfg;
        this.patchLocale(cfg);
        this.patchReceipt(cfg);
        this.pageState.set('ready');
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.pageError.set(pd?.title ?? 'Erreur de chargement.');
        this.pageState.set('error');
      },
    });
  }

  saveLocale(): void {
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
    this.api.updateInternalSettings(updated).subscribe({
      next: () => {
        this.config = updated;
        this.saveState.set('idle');
        this.snackBar.open('Paramètres de langue enregistrés.', 'OK', { duration: 3000 });
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.snackBar.open(pd?.title ?? 'Erreur lors de la sauvegarde.', 'OK', { duration: 5000 });
        this.saveState.set('idle');
      },
    });
  }

  saveReceipt(): void {
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
    this.api.updateInternalSettings(updated).subscribe({
      next: () => {
        this.config = updated;
        this.receiptSaveState.set('idle');
        this.snackBar.open('Configuration du reçu enregistrée.', 'OK', { duration: 3000 });
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.snackBar.open(pd?.title ?? 'Erreur lors de la sauvegarde.', 'OK', { duration: 5000 });
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
}
