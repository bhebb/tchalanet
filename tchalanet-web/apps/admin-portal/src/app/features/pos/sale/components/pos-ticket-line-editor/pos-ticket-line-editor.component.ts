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
  PosGameView,
  PosTicketDraftLine,
  PosTicketLineInput,
} from '../../data-access/pos-sale.models';

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
  readonly readonly = input(false);

  readonly lineAdded = output<PosTicketLineInput>();
  readonly lineRemoved = output<string>();
  readonly lineStakeUpdated = output<{ localId: string; stakeAmount: number }>();
  readonly linesCleared = output<void>();

  // Draft new line state
  readonly draftSelection = signal('');
  readonly draftBetType = signal('');
  readonly draftStake = signal<number | null>(null);
  readonly editingLineId = signal<string | null>(null);
  readonly editingStake = signal<number | null>(null);

  readonly selectedGame = computed(() => {
    const selectedCode = this.selectedGameCode();
    return this.games().find(game => game.gameCode === selectedCode) ?? null;
  });

  readonly betTypes = computed(() => this.selectedGame()?.betTypes ?? []);

  readonly selectedBetType = computed(() => {
    const current = this.draftBetType();
    const betTypes = this.betTypes();
    return betTypes.find(betType => betType.betType === current) ?? betTypes[0] ?? null;
  });

  readonly selectedBetTypeCode = computed(() => this.selectedBetType()?.betType ?? '');
  readonly selectionMaxLength = computed(() => {
    const betType = this.selectedBetType()?.betType ?? '';
    if (betType === 'MARRIAGE_2D2D') return 5;
    return this.selectionWidth(betType) ?? 12;
  });

  readonly canAdd = computed(() => {
    const sel = this.draftSelection().trim();
    const stake = this.draftStake();
    const gameCode = this.selectedGameCode();
    return !this.readonly() && sel.length > 0 && stake !== null && stake > 0 && !!gameCode && !!this.selectedBetType();
  });

  readonly duplicateDraftLine = computed(() => {
    const gameCode = this.selectedGameCode();
    const betType = this.selectedBetType();
    const selection = this.normalizeSelection(this.draftSelection(), betType?.betType ?? '');
    if (!gameCode || !betType || !selection) return null;

    return this.lines().find(line =>
      line.gameCode === gameCode &&
      line.betType === betType.betType &&
      line.selection === selection,
    ) ?? null;
  });

  readonly projectedStakeAmount = computed(() => {
    const duplicate = this.duplicateDraftLine();
    const stake = this.draftStake();
    if (!duplicate || !stake || stake <= 0) return null;
    return duplicate.stakeAmount + stake;
  });

  readonly addButtonLabel = computed(() =>
    this.duplicateDraftLine() ? 'Ajouter à la ligne' : 'Ajouter',
  );

  readonly addError = signal<string | null>(null);

  addLine(): void {
    const sel = this.draftSelection().trim();
    const stake = this.draftStake();
    const gameCode = this.selectedGameCode();
    const betType = this.selectedBetType();
    const selection = this.normalizeSelection(sel, betType?.betType ?? '');

    if (this.readonly()) return;
    if (!sel) { this.addError.set('Le numéro est requis.'); return; }
    if (!stake || stake <= 0) { this.addError.set('La mise doit être supérieure à 0 HTG.'); return; }
    if (!gameCode) { this.addError.set('Sélectionnez un jeu.'); return; }
    if (!betType) { this.addError.set('Aucun type de pari disponible pour ce jeu.'); return; }
    if (!selection) { this.addError.set(this.selectionErrorMessage(betType.betType)); return; }

    this.addError.set(null);
    this.lineAdded.emit({
      gameCode,
      selection,
      betType: betType.betType,
      betTypeLabel: betType.label,
      stakeAmount: stake,
    });
    this.draftSelection.set('');
    this.draftStake.set(null);
  }

  onDraftKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') this.addLine();
  }

  removeLine(localId: string): void {
    if (this.readonly()) return;
    this.lineRemoved.emit(localId);
  }

  startEdit(line: PosTicketDraftLine): void {
    if (this.readonly()) return;
    this.editingLineId.set(line.localId);
    this.editingStake.set(line.stakeAmount);
  }

  cancelEdit(): void {
    this.editingLineId.set(null);
    this.editingStake.set(null);
  }

  saveEdit(line: PosTicketDraftLine): void {
    const stake = this.editingStake();
    if (this.readonly() || stake === null || stake <= 0) return;
    this.lineStakeUpdated.emit({ localId: line.localId, stakeAmount: stake });
    this.cancelEdit();
  }

  private normalizeSelection(selection: string, betType: string): string | null {
    const digits = selection.replace(/\D/g, '');
    const width = this.selectionWidth(betType);
    if (!width) return selection.trim();
    if (digits.length === 0 || digits.length > width) return null;
    return digits.padStart(width, '0');
  }

  private selectionWidth(betType: string): number | null {
    switch (betType) {
      case 'MATCH_1_2D':
      case 'MATCH_2_2D':
      case 'MATCH_3_2D':
        return 2;
      case 'LOTTO3_3D':
        return 3;
      case 'LOTTO4_PATTERN':
        return 4;
      case 'LOTTO5_PATTERN':
        return 5;
      default:
        return null;
    }
  }

  private selectionErrorMessage(betType: string): string {
    const width = this.selectionWidth(betType);
    if (!width) return 'La sélection est invalide.';
    return `Entrez un numéro de ${width} chiffre${width > 1 ? 's' : ''}.`;
  }
}
