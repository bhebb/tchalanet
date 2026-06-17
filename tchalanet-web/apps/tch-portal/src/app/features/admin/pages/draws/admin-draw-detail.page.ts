import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, merge, of, startWith, Subject, switchMap } from 'rxjs';
import { finalize } from 'rxjs/operators';
import {
  TchLoading,
  TchErrorPanel,
  AdminPageHeader,
  AdminFormSection,
} from '@tch/ui/components';

import { DrawAdminApi, DrawStatus, DrawSummary } from '../../draw-admin.api.service';

type PageState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly draw: DrawSummary };

const STATUS_LABELS: Record<DrawStatus, string> = {
  SCHEDULED: 'Planifié',
  OPEN: 'Ouvert',
  CLOSED: 'Fermé',
  RESULTED: 'Résulté',
  SETTLED: 'Réglé',
  CANCELED: 'Annulé',
  ARCHIVED: 'Archivé',
};

@Component({
  selector: 'tch-admin-draw-detail-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    TchLoading,
    TchErrorPanel,
    AdminPageHeader,
    AdminFormSection,
  ],
  templateUrl: './admin-draw-detail.page.html',
  styleUrl: './admin-draw-detail.page.scss',
})
export class AdminDrawDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(DrawAdminApi);
  private readonly fb = inject(FormBuilder);

  private readonly drawId = toSignal(
    this.route.paramMap.pipe(map(p => p.get('id') ?? '')),
    { initialValue: '' },
  );

  private readonly refresh$ = new Subject<void>();

  readonly state = toSignal(
    merge(toObservable(this.drawId), this.refresh$).pipe(
      switchMap(() => {
        const id = this.drawId();
        return id
          ? this.api.getDrawById(id).pipe(
              map(draw => ({ status: 'ready', draw }) as PageState),
              catchError(() => of({ status: 'error' } as PageState)),
              startWith({ status: 'loading' } as PageState),
            )
          : of({ status: 'error' } as PageState);
      }),
    ),
    { initialValue: { status: 'loading' } as PageState },
  );

  readonly resultForm = this.fb.nonNullable.group({
    pick3: [''],
    pick4: [''],
    notes: [''],
  });

  readonly resultPending = signal(false);
  readonly resultError = signal<string | null>(null);

  private static readonly RESULT_STATUS_LABELS: Record<string, string> = {
    PROVISIONAL: 'Provisoire — en attente de validation plateforme',
    CONFIRMED: 'Confirmé',
    OVERRIDDEN: 'Corrigé',
    ERROR: 'Erreur',
  };

  statusLabel(status: DrawStatus): string {
    return STATUS_LABELS[status] ?? status;
  }

  resultStatusLabel(status: string): string {
    return AdminDrawDetailPage.RESULT_STATUS_LABELS[status] ?? status;
  }

  isResultProvisional(status: string): boolean {
    return status === 'PROVISIONAL';
  }

  hasAnyPick(): boolean {
    const { pick3, pick4 } = this.resultForm.getRawValue();
    return (pick3?.trim().length ?? 0) > 0 || (pick4?.trim().length ?? 0) > 0;
  }

  submitResult(draw: DrawSummary): void {
    if (!this.hasAnyPick()) return;

    const { pick3, pick4, notes } = this.resultForm.getRawValue();
    this.resultPending.set(true);
    this.resultError.set(null);

    this.api
      .proposeManualResult({
        drawDate: draw.drawDate,
        slotKey: draw.slot.key,
        pick3: pick3?.trim() || undefined,
        pick4: pick4?.trim() || undefined,
        notes: notes?.trim() || undefined,
      })
      .pipe(finalize(() => this.resultPending.set(false)))
      .subscribe({
        next: () => {
          this.resultForm.reset();
          this.refresh$.next();
        },
        error: () => this.resultError.set('Une erreur est survenue. Vérifiez les valeurs et réessayez.'),
      });
  }
}
