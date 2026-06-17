import {
  ChangeDetectionStrategy,
  Component,
  inject,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import {
  TchLoading,
  TchErrorPanel,
  AdminPageHeader,
  AdminFormSection,
  AdminStatusBadge,
  AdminConfirmDialog,
  type AdminConfirmDialogData,
} from '@tch/ui/components';
import { catchError, map, of, startWith, switchMap } from 'rxjs';

import {
  SellerTerminalAdminApi,
  SellerTerminalRow,
  SellerTerminalStatus,
} from '../../seller-terminal-admin.api.service';
import { ResetPinDialog } from './reset-pin.dialog';

type PageState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly seller: SellerTerminalRow };

function initials(name: string): string {
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map(w => w[0]?.toUpperCase() ?? '')
    .join('');
}

const STATUS_LABEL: Record<SellerTerminalStatus, string> = {
  ACTIVE: 'Actif',
  PENDING: 'En attente',
  BLOCKED: 'Bloqué',
  DISABLED: 'Désactivé',
};

@Component({
  selector: 'tch-admin-seller-detail-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    DecimalPipe,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    TchLoading,
    TchErrorPanel,
    AdminPageHeader,
    AdminFormSection,
    AdminStatusBadge,
  ],
  templateUrl: './admin-seller-detail.page.html',
  styleUrl: './admin-seller-detail.page.scss',
})
export class AdminSellerDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(SellerTerminalAdminApi);
  private readonly dialog = inject(MatDialog);
  protected readonly router = inject(Router);

  protected readonly avatarInitials = initials;

  private readonly sellerId = toSignal(
    this.route.paramMap.pipe(map(p => p.get('id') ?? '')),
    { initialValue: '' },
  );

  readonly state = toSignal(
    toObservable(this.sellerId).pipe(
      switchMap(id =>
        id
          ? this.api.getById(id).pipe(
              map(seller => ({ status: 'ready', seller }) as PageState),
              catchError(() => of({ status: 'error' } as PageState)),
              startWith({ status: 'loading' } as PageState),
            )
          : of({ status: 'error' } as PageState),
      ),
    ),
    { initialValue: { status: 'loading' } as PageState },
  );

  statusLabel(status: SellerTerminalStatus): string {
    return STATUS_LABEL[status] ?? status;
  }

  block(seller: SellerTerminalRow): void {
    const data: AdminConfirmDialogData = {
      title: 'Bloquer ce vendeur',
      message: `L'accès de ${seller.displayName} sera suspendu. Il ne pourra plus se connecter jusqu'au déblocage.`,
      confirmLabel: 'Bloquer',
      destructive: true,
    };
    this.dialog
      .open(AdminConfirmDialog, { data, width: '400px' })
      .afterClosed()
      .subscribe(confirmed => {
        if (confirmed) {
          this.api
            .block(seller.id, 'Bloqué manuellement par l\'administrateur')
            .subscribe({ next: () => this.refresh() });
        }
      });
  }

  unblock(seller: SellerTerminalRow): void {
    const data: AdminConfirmDialogData = {
      title: 'Débloquer ce vendeur',
      message: `Voulez-vous rétablir l'accès de ${seller.displayName} ?`,
      confirmLabel: 'Débloquer',
    };
    this.dialog
      .open(AdminConfirmDialog, { data, width: '400px' })
      .afterClosed()
      .subscribe(confirmed => {
        if (confirmed) {
          this.api.unblock(seller.id).subscribe({ next: () => this.refresh() });
        }
      });
  }

  openResetPin(seller: SellerTerminalRow): void {
    this.dialog
      .open(ResetPinDialog, {
        data: { sellerName: seller.displayName },
        width: '360px',
      })
      .afterClosed()
      .subscribe(result => {
        if (result?.pin) {
          this.api.resetPin(seller.id, result.pin).subscribe();
        }
      });
  }

  private refresh(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      onSameUrlNavigation: 'reload',
    });
  }
}
