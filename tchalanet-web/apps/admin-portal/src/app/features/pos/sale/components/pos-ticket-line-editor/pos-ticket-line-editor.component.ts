import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  signal,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import {
  PosBetType,
  PosGameView,
  PosTicketDraftLine,
  PosTicketLineInput,
} from '../../data-access/pos-sale.models';

export const BET_TYPE_LABELS: Record<PosBetType, string> = {
  DIRECT: 'Direct',
  BOUL: 'Boul',
  MARYAJ: 'Maryaj',
};

@Component({
  selector: 'tch-pos-ticket-line-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './pos-ticket-line-editor.component.html',
  styleUrls: ['./pos-ticket-line-editor.component.scss'],
})
export class PosTicketLineEditorComponent {
  readonly lines = input<PosTicketDraftLine[]>([]);
  readonly selectedGameCode = input<string | null>(null);
  readonly games = input<PosGameView[]>([]);

  readonly lineAdded = output<PosTicketLineInput>();
  readonly lineRemoved = output<string>();
  readonly linesCleared = output<void>();

  // Draft new line state
  readonly draftSelection = signal('');
  readonly draftBetType = signal<PosBetType>('DIRECT');
  readonly draftStake = signal<number | null>(null);

  readonly betTypes: PosBetType[] = ['DIRECT', 'BOUL', 'MARYAJ'];
  readonly betTypeLabels = BET_TYPE_LABELS;

  readonly canAdd = computed(() => {
    const sel = this.draftSelection().trim();
    const stake = this.draftStake();
    const gameCode = this.selectedGameCode();
    return sel.length > 0 && stake !== null && stake > 0 && !!gameCode;
  });

  readonly addError = signal<string | null>(null);

  addLine(): void {
    const sel = this.draftSelection().trim();
    const stake = this.draftStake();
    const gameCode = this.selectedGameCode();

    if (!sel) { this.addError.set('Le numéro est requis.'); return; }
    if (!stake || stake <= 0) { this.addError.set('La mise doit être supérieure à 0 HTG.'); return; }
    if (!gameCode) { this.addError.set('Sélectionnez un jeu.'); return; }

    this.addError.set(null);
    this.lineAdded.emit({
      gameCode,
      selection: sel,
      betType: this.draftBetType(),
      stakeAmount: stake,
    });
    this.draftSelection.set('');
    this.draftStake.set(null);
  }

  onDraftKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') this.addLine();
  }
}
