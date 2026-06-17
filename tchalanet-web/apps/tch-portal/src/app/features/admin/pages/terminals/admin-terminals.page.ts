import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../private/shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../private/shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../private/shared/admin-ui/admin-section-card.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../private/shared/admin-ui/admin-status-pill.component';
import {
  CreateSellerTerminalRequest,
  SellerTerminalApi,
  SellerTerminalSummaryRow,
  SellerTerminalStatus,
} from '../../seller-terminal-api.service';

// ── Dialog ────────────────────────────────────────────────────────────────────

interface CreateDialogResult {
  reload: boolean;
}

@Component({
  selector: 'tch-create-seller-terminal-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    AdminSectionCardComponent,
    TchErrorPanel,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <mat-dialog-content>
      <tch-admin-section-card title="Nouveau vendeur" icon="person_add">
        <form [formGroup]="form" (ngSubmit)="submit()" class="dialog-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Code terminal</mat-label>
            <input matInput formControlName="terminalCode" placeholder="ex: VND-001" />
            @if (form.controls.terminalCode.invalid && form.controls.terminalCode.touched) {
              <mat-error>Code requis (max 64 caractères).</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Nom affiché</mat-label>
            <input matInput formControlName="displayName" placeholder="ex: Jean Pierre" />
            @if (form.controls.displayName.invalid && form.controls.displayName.touched) {
              <mat-error>Nom requis (max 180 caractères).</mat-error>
            }
          </mat-form-field>

          <div class="two-col">
            <mat-form-field appearance="outline">
              <mat-label>Prénom (optionnel)</mat-label>
              <input matInput formControlName="firstName" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Nom de famille (optionnel)</mat-label>
              <input matInput formControlName="lastName" />
            </mat-form-field>
          </div>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Numéro de téléphone (optionnel)</mat-label>
            <input matInput formControlName="phoneNumber" type="tel" placeholder="+509 XXXX XXXX" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Taux de commission % (optionnel)</mat-label>
            <input
              matInput
              formControlName="commissionRate"
              type="number"
              min="0"
              max="100"
              step="0.01"
            />
            @if (form.controls.commissionRate.invalid && form.controls.commissionRate.touched) {
              <mat-error>Taux entre 0 et 100.</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>PIN initial</mat-label>
            <input
              matInput
              formControlName="initialPin"
              type="password"
              placeholder="4 à 8 chiffres"
            />
            @if (form.controls.initialPin.invalid && form.controls.initialPin.touched) {
              <mat-error>PIN requis : 4 à 8 chiffres uniquement.</mat-error>
            }
          </mat-form-field>

          @if (error()) {
            <tch-error-panel [title]="error()!" />
          }

          <div class="dialog-actions">
            <button mat-button type="button" mat-dialog-close>Annuler</button>
            <button
              mat-flat-button
              color="primary"
              type="submit"
              [disabled]="form.invalid || saving()"
            >
              @if (saving()) {
                <span class="material-symbols-outlined spin" aria-hidden="true">
                  progress_activity
                </span>
              }
              Créer
            </button>
          </div>
        </form>
      </tch-admin-section-card>
    </mat-dialog-content>
  `,
  styles: [
    `
      mat-dialog-content {
        padding: 0 !important;
        max-width: 560px;
      }

      .dialog-form {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }

      .full-width {
        width: 100%;
      }

      .two-col {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 0.75rem;
      }

      .dialog-actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.75rem;
        margin-top: 0.5rem;
      }

      .spin {
        animation: spin 0.8s linear infinite;
        display: inline-block;
        vertical-align: middle;
        font-family: 'Material Symbols Outlined';
      }

      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }
    `,
  ],
})
export class CreateSellerTerminalDialog {
  private readonly api = inject(SellerTerminalApi);
  private readonly dialogRef = inject<MatDialogRef<CreateSellerTerminalDialog, CreateDialogResult>>(MatDialogRef);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    terminalCode: ['', [Validators.required, Validators.maxLength(64)]],
    displayName: ['', [Validators.required, Validators.maxLength(180)]],
    firstName: [''],
    lastName: [''],
    phoneNumber: [''],
    commissionRate: [null as number | null, [Validators.min(0), Validators.max(100)]],
    initialPin: ['', [Validators.required, Validators.pattern(/^\d{4,8}$/)]],
  });

  submit(): void {
    if (this.form.invalid || this.saving()) return;

    const v = this.form.value;
    const req: CreateSellerTerminalRequest = {
      terminalCode: v.terminalCode!,
      displayName: v.displayName!,
      firstName: v.firstName || null,
      lastName: v.lastName || null,
      phoneNumber: v.phoneNumber || null,
      commissionRate: v.commissionRate ?? null,
      initialPin: v.initialPin!,
    };

    this.saving.set(true);
    this.error.set(null);

    this.api.create(req).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Vendeur créé avec succès.', 'OK', { duration: 3000 });
        this.dialogRef.close({ reload: true });
      },
      error: (err: unknown) => {
        this.saving.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors de la création.');
      },
    });
  }
}

// ── Page ─────────────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-admin-terminals-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminPageShellComponent,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatSelectModule,
    MatTableModule,
  ],
  template: `
    <tch-admin-page-shell
      title="Vendeurs / Terminaux"
      description="Gérez les vendeurs et terminaux de vente."
    >
      <div actions>
        <button mat-flat-button color="primary" (click)="openCreate()">
          <span class="material-symbols-outlined">add</span>
          Nouveau vendeur
        </button>
      </div>

      @if (loading()) {
        <tch-loading label="Chargement..." />
      } @else if (error()) {
        <tch-error-panel
          [title]="error()!"
          [showRetry]="true"
          retryLabel="Réessayer"
          (retry)="loadPage()"
        />
      } @else {
        <tch-admin-crud-shell>
          <ng-container toolbar>
            <tch-admin-data-toolbar
              searchPlaceholder="Rechercher un vendeur..."
              [searchValue]="searchQuery()"
              (searchChange)="onSearch($event)"
            >
              <mat-select
                [value]="statusFilter()"
                (selectionChange)="onStatusFilter($event.value)"
                class="status-select"
              >
                <mat-option value="">Tous les statuts</mat-option>
                <mat-option value="ACTIVE">Actif</mat-option>
                <mat-option value="INACTIVE">Inactif</mat-option>
                <mat-option value="BLOCKED">Bloqué</mat-option>
                <mat-option value="DISABLED">Désactivé</mat-option>
              </mat-select>
            </tch-admin-data-toolbar>
          </ng-container>

          <ng-container content>
            @if (items().length === 0) {
              <tch-admin-empty-state
                icon="point_of_sale"
                title="Aucun vendeur enregistré"
                message="Créez un premier vendeur pour commencer."
              />
            } @else {
              <div class="table-container">
                <table mat-table [dataSource]="items()" class="terminals-table">
                  <ng-container matColumnDef="terminalCode">
                    <th mat-header-cell *matHeaderCellDef>Code</th>
                    <td mat-cell *matCellDef="let row">{{ row.terminalCode }}</td>
                  </ng-container>

                  <ng-container matColumnDef="displayName">
                    <th mat-header-cell *matHeaderCellDef>Nom</th>
                    <td mat-cell *matCellDef="let row">{{ row.displayName }}</td>
                  </ng-container>

                  <ng-container matColumnDef="phoneNumber">
                    <th mat-header-cell *matHeaderCellDef>Téléphone</th>
                    <td mat-cell *matCellDef="let row">{{ row.phoneNumber ?? '—' }}</td>
                  </ng-container>

                  <ng-container matColumnDef="status">
                    <th mat-header-cell *matHeaderCellDef>Statut</th>
                    <td mat-cell *matCellDef="let row">
                      <tch-admin-status-pill
                        [label]="row.status"
                        [tone]="statusTone(row.status)"
                      />
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="commissionRate">
                    <th mat-header-cell *matHeaderCellDef>Commission</th>
                    <td mat-cell *matCellDef="let row">
                      {{ row.commissionRate != null ? row.commissionRate + '%' : '—' }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="lastSeenAt">
                    <th mat-header-cell *matHeaderCellDef>Dernière activité</th>
                    <td mat-cell *matCellDef="let row">
                      {{ row.lastSeenAt ? (row.lastSeenAt | date: 'dd/MM/yyyy HH:mm') : '—' }}
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="actions">
                    <th mat-header-cell *matHeaderCellDef></th>
                    <td mat-cell *matCellDef="let row">
                      <button
                        mat-icon-button
                        [matMenuTriggerFor]="rowMenu"
                        [matMenuTriggerData]="{ row }"
                      >
                        <span class="material-symbols-outlined">more_vert</span>
                      </button>
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
                </table>
              </div>
            }

            <mat-menu #rowMenu="matMenu">
              <ng-template matMenuContent let-row="row">
                @if (row.status === 'ACTIVE') {
                  <button mat-menu-item (click)="block(row)">
                    <span class="material-symbols-outlined">block</span>
                    Bloquer
                  </button>
                }
                @if (row.status === 'BLOCKED') {
                  <button mat-menu-item (click)="unblock(row)">
                    <span class="material-symbols-outlined">lock_open</span>
                    Débloquer
                  </button>
                }
                <button mat-menu-item (click)="resetPin(row)">
                  <span class="material-symbols-outlined">key</span>
                  Reset PIN
                </button>
                @if (row.status !== 'DISABLED') {
                  <button mat-menu-item (click)="disable(row)">
                    <span class="material-symbols-outlined">do_not_disturb_on</span>
                    Désactiver
                  </button>
                }
              </ng-template>
            </mat-menu>
          </ng-container>

          <ng-container footer>
            <div class="pagination">
              <button mat-button [disabled]="page() === 0" (click)="prevPage()">
                <span class="material-symbols-outlined">chevron_left</span>
                Précédent
              </button>
              <span>Page {{ page() + 1 }} / {{ totalPages() }}</span>
              <button mat-button [disabled]="page() + 1 >= totalPages()" (click)="nextPage()">
                Suivant
                <span class="material-symbols-outlined">chevron_right</span>
              </button>
            </div>
          </ng-container>
        </tch-admin-crud-shell>
      }
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .table-container {
        overflow-x: auto;
      }

      .terminals-table {
        width: 100%;
      }

      .status-select {
        min-width: 160px;
      }

      .pagination {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 1rem;
      }

      .material-symbols-outlined {
        font-family: 'Material Symbols Outlined';
      }
    `,
  ],
})
export class AdminTerminalsPage implements OnInit {
  private readonly api = inject(SellerTerminalApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = [
    'terminalCode',
    'displayName',
    'phoneNumber',
    'status',
    'commissionRate',
    'lastSeenAt',
    'actions',
  ];

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly items = signal<SellerTerminalSummaryRow[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly searchQuery = signal('');
  readonly statusFilter = signal<SellerTerminalStatus | ''>('');

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / 20)));

  ngOnInit(): void {
    this.loadPage();
  }

  loadPage(): void {
    this.loading.set(true);
    this.error.set(null);

    this.api
      .list({
        q: this.searchQuery() || undefined,
        status: this.statusFilter() || undefined,
        page: this.page(),
        size: 20,
      })
      .subscribe({
        next: res => {
          this.items.set(res.items);
          this.total.set(res.total);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          const pd = (err as { error?: { title?: string } })?.error;
          this.error.set(pd?.title ?? 'Erreur de chargement.');
          this.loading.set(false);
        },
      });
  }

  onSearch(q: string): void {
    this.searchQuery.set(q);
    this.page.set(0);
    this.loadPage();
  }

  onStatusFilter(status: SellerTerminalStatus | ''): void {
    this.statusFilter.set(status);
    this.page.set(0);
    this.loadPage();
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.page.update(p => p - 1);
      this.loadPage();
    }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.page.update(p => p + 1);
      this.loadPage();
    }
  }

  openCreate(): void {
    const ref = this.dialog.open<CreateSellerTerminalDialog, void, CreateDialogResult>(
      CreateSellerTerminalDialog,
      { width: '600px' },
    );
    ref.afterClosed().subscribe(result => {
      if (result?.reload) {
        this.loadPage();
      }
    });
  }

  block(row: SellerTerminalSummaryRow): void {
    this.api.block(row.id.value, 'Bloqué par un administrateur').subscribe({
      next: () => {
        this.snackBar.open(`${row.displayName} bloqué.`, 'OK', { duration: 3000 });
        this.loadPage();
      },
      error: () =>
        this.snackBar.open('Erreur lors du blocage.', 'OK', { duration: 4000 }),
    });
  }

  unblock(row: SellerTerminalSummaryRow): void {
    this.api.unblock(row.id.value).subscribe({
      next: () => {
        this.snackBar.open(`${row.displayName} débloqué.`, 'OK', { duration: 3000 });
        this.loadPage();
      },
      error: () =>
        this.snackBar.open('Erreur lors du déblocage.', 'OK', { duration: 4000 }),
    });
  }

  resetPin(row: SellerTerminalSummaryRow): void {
    const newPin = prompt('Nouveau PIN (4–8 chiffres) :');
    if (!newPin || !/^\d{4,8}$/.test(newPin)) {
      if (newPin !== null) {
        this.snackBar.open('PIN invalide. Format : 4–8 chiffres.', 'OK', { duration: 4000 });
      }
      return;
    }

    this.api.resetAccess(row.id.value, newPin).subscribe({
      next: () =>
        this.snackBar.open('PIN réinitialisé.', 'OK', { duration: 3000 }),
      error: () =>
        this.snackBar.open('Erreur lors de la réinitialisation du PIN.', 'OK', { duration: 4000 }),
    });
  }

  disable(row: SellerTerminalSummaryRow): void {
    const confirmed = confirm(
      `Désactiver "${row.displayName}" ? Cette action est difficile à annuler.`,
    );
    if (!confirmed) return;

    this.api.disable(row.id.value).subscribe({
      next: () => {
        this.snackBar.open(`${row.displayName} désactivé.`, 'OK', { duration: 3000 });
        this.loadPage();
      },
      error: () =>
        this.snackBar.open('Erreur lors de la désactivation.', 'OK', { duration: 4000 }),
    });
  }

  statusTone(status: SellerTerminalStatus): AdminStatusTone {
    if (status === 'ACTIVE') return 'success';
    if (status === 'BLOCKED') return 'danger';
    if (status === 'DISABLED') return 'danger';
    return 'neutral';
  }
}
