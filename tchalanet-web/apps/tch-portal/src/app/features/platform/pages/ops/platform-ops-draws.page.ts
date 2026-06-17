import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import {
  PlatformOpsApi,
  OpenTodayDrawsRequest,
  CloseDueDrawsRequest,
} from '../../platform-ops-api.service';
import { GenerateDrawsDialog } from './dialogs/generate-draws.dialog';
import { BatchOpDialog, AnyBatchResult } from './dialogs/batch-op.dialog';
import { ApplyResultsDialog } from './dialogs/apply-results.dialog';

interface DrawOpCard {
  id: string;
  icon: string;
  title: string;
  description: string;
  open: () => void;
}

@Component({
  selector: 'tch-platform-ops-draws-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './platform-ops-draws.page.html',
  styleUrls: ['./platform-ops-draws.page.scss'],
})
export class PlatformOpsDrawsPage {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);

  readonly drawActions: DrawOpCard[] = [
    {
      id: 'generate',
      icon: 'event_note',
      title: 'Générer les tirages',
      description: 'Crée les tirages pour une plage de dates. Idempotent — ignore ceux déjà existants.',
      open: () => this.dialog.open(GenerateDrawsDialog, { width: '520px' }),
    },
    {
      id: 'open-today',
      icon: 'lock_open',
      title: "Ouvrir les tirages du jour",
      description: "Ouvre tous les tirages dont la date est aujourd'hui (miroir du scheduler).",
      open: () => this.openBatch("Ouvrir les tirages du jour", true, (tenantCodes, dryRun, limit) => {
        const req: OpenTodayDrawsRequest = { tenantCodes, limit, dryRun };
        this.api.openTodayDraws(req).subscribe({
          next: (res) => this.currentBatchDialog?.setResult(res as AnyBatchResult),
          error: (err: unknown) => {
            const pd = (err as { error?: { title?: string } })?.error;
            this.currentBatchDialog?.setError(pd?.title ?? 'Erreur.');
          },
        });
      }),
    },
    {
      id: 'close-due',
      icon: 'lock',
      title: 'Fermer les tirages échus',
      description: "Ferme tous les tirages dont l'heure de clôture est dépassée.",
      open: () => this.openBatch("Fermer les tirages échus", true, (tenantCodes, dryRun, limit) => {
        const req: CloseDueDrawsRequest = { tenantCodes, limit, dryRun };
        this.api.closeDueDraws(req).subscribe({
          next: (res) => this.currentBatchDialog?.setResult(res as AnyBatchResult),
          error: (err: unknown) => {
            const pd = (err as { error?: { title?: string } })?.error;
            this.currentBatchDialog?.setError(pd?.title ?? 'Erreur.');
          },
        });
      }),
    },
    {
      id: 'apply',
      icon: 'assignment_turned_in',
      title: 'Appliquer les résultats',
      description: 'Applique les résultats confirmés aux tirages et déclenche le règlement.',
      open: () => this.dialog.open(ApplyResultsDialog, { width: '460px' }),
    },
  ];

  private currentBatchDialog: BatchOpDialog | null = null;

  private openBatch(
    title: string,
    hasLimit: boolean,
    execute: (tenantCodes: string[], dryRun: boolean, limit?: number) => void,
  ): void {
    const ref = this.dialog.open(BatchOpDialog, {
      data: { title, hasLimit, execute },
      width: '500px',
    });
    this.currentBatchDialog = ref.componentInstance;
    ref.afterClosed().subscribe(() => { this.currentBatchDialog = null; });
  }
}
