import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../../shared/admin-ui/admin-status-pill.component';
import { RuntimeApiService, TenantRuntimeView } from '../../data-access/runtime-api.service';

const LANGUAGE_LABELS: Record<string, string> = {
  fr: 'Français',
  en: 'English',
  ht: 'Kreyòl Ayisyen',
};

@Component({
  selector: 'tch-admin-runtime-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminSectionCardComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './admin-runtime.page.html',
  styleUrls: ['./admin-runtime.page.scss'],
})
export class AdminRuntimePage implements OnInit {
  private readonly api = inject(RuntimeApiService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly runtime = signal<TenantRuntimeView | null>(null);
  readonly runtimeJson = () =>
    this.runtime() ? JSON.stringify(this.runtime(), null, 2) : '';

  languageLabel(code: string): string {
    return LANGUAGE_LABELS[code] ?? code;
  }

  runtimeStatusTone(status: string): AdminStatusTone {
    const map: Record<string, AdminStatusTone> = {
      ACTIVE: 'success',
      DRAFT: 'neutral',
      SUSPENDED: 'warning',
      ARCHIVED: 'danger',
    };
    return map[status] ?? 'neutral';
  }

  ngOnInit(): void {
    this.load();
  }

  reload(): void {
    this.load();
    this.snackBar.open('Données rechargées.', 'OK', { duration: 2000 });
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getTenantRuntime().subscribe({
      next: v => { this.runtime.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }
}
