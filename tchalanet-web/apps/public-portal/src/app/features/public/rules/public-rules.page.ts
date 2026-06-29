import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LabelPipe } from '@tch/page-model';
import { TCH_PUBLIC_ASSETS } from '@tch/shared-assets';

import { PUBLIC_RULE_GAMES, PUBLIC_TCHALA_ENTRIES } from './public-rules.data';
import type { PublicRuleGameId } from './public-rules.model';
import {
  SESSION_AMOUNT_KEY,
  SESSION_GAME_KEY,
  initialOption,
  sessionReadAmount,
  sessionReadGame,
  sessionWrite,
} from './public-rules.storage';
import { filterTchalaEntries } from './public-rules.utils';

@Component({
  selector: 'tch-public-rules-page',
  imports: [DecimalPipe, LabelPipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-rules.page.html',
  styleUrls: ['./public-rules.page.scss'],
})
export class PublicRulesPage {
  readonly games = PUBLIC_RULE_GAMES;
  readonly heroImage = TCH_PUBLIC_ASSETS.rulesTchalaPreview;
  readonly query = signal('');
  readonly selectedGame = signal<PublicRuleGameId>(sessionReadGame());
  readonly selectedOption = signal<string>(initialOption());
  readonly betAmount = signal(sessionReadAmount());
  readonly multiplierOverride = signal<number | null>(null);

  readonly filteredEntries = computed(() =>
    filterTchalaEntries(PUBLIC_TCHALA_ENTRIES, this.query()),
  );

  readonly activeGame = computed(() => this.games.find(g => g.id === this.selectedGame())!);

  readonly activeOption = computed(() => {
    const game = this.activeGame();
    return game.betOptions.find(o => o.id === this.selectedOption()) ?? game.betOptions[0];
  });

  readonly effectiveMultiplier = computed(
    () => this.multiplierOverride() ?? this.activeOption().defaultMultiplier,
  );

  readonly isMultiplierCustom = computed(() => this.multiplierOverride() !== null);

  readonly displayedMultiplier = computed(() => {
    if (this.selectedGame() === 'maryaj_gratis') {
      return this.multiplierOverride() ?? this.activeOption().bonusMultiplier ?? 0;
    }
    return this.effectiveMultiplier();
  });

  readonly borletteBaseGain = computed(
    () => this.betAmount() * this.activeOption().defaultMultiplier,
  );

  readonly maryajGratisBonus = computed(() => {
    const bonus = this.multiplierOverride() ?? this.activeOption().bonusMultiplier ?? 0;
    return this.betAmount() * bonus;
  });

  readonly potentialGain = computed(() => {
    if (this.selectedGame() === 'maryaj_gratis') {
      return this.borletteBaseGain() + this.maryajGratisBonus();
    }
    return this.betAmount() * this.effectiveMultiplier();
  });

  updateQuery(event: Event): void {
    const target = event.target;
    this.query.set(target instanceof HTMLInputElement ? target.value : '');
  }

  selectGame(id: PublicRuleGameId): void {
    const game = PUBLIC_RULE_GAMES.find(g => g.id === id);
    this.selectedGame.set(id);
    this.selectedOption.set(game?.betOptions[0]?.id ?? '');
    this.multiplierOverride.set(null);
    sessionWrite(SESSION_GAME_KEY, id);
  }

  selectBetOption(id: string): void {
    this.selectedOption.set(id);
    this.multiplierOverride.set(null);
  }

  updateBetAmount(event: Event): void {
    const value = parseInt((event.target as HTMLInputElement).value, 10);
    if (Number.isFinite(value) && value > 0) {
      this.betAmount.set(value);
      sessionWrite(SESSION_AMOUNT_KEY, String(value));
    }
  }

  updateMultiplier(event: Event): void {
    const value = parseInt((event.target as HTMLInputElement).value, 10);
    if (Number.isFinite(value) && value > 0) {
      this.multiplierOverride.set(value);
    }
  }

  resetMultiplier(): void {
    this.multiplierOverride.set(null);
  }
}
