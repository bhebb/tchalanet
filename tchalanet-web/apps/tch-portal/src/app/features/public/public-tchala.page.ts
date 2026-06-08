import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';

import { LabelPipe } from '@tch/page-model';

interface TchalaEntry {
  readonly id: string;
  readonly letter: string;
  readonly icon: string;
  readonly term: string;
  readonly description: string;
  readonly numbers: readonly string[];
}

const TCHALA_ENTRIES: readonly TchalaEntry[] = [
  { id: 'airplane', letter: 'A', icon: 'flight', term: 'Avion', description: 'Vwayaj, ambisyon, elvasyou.', numbers: ['04', '71'] },
  { id: 'alligator', letter: 'A', icon: 'water', term: 'Alligator', description: 'Danje kache, pouvwa ki an sekirite.', numbers: ['21', '47'] },
  { id: 'argent', letter: 'A', icon: 'account_balance_wallet', term: 'Argent', description: 'Lajan, resous, abondans.', numbers: ['33', '67'] },
  { id: 'baby', letter: 'B', icon: 'child_care', term: 'Bébé', description: 'Nouvo kòmansman, pwoteksyon.', numbers: ['08', '55'] },
  { id: 'bateau', letter: 'B', icon: 'directions_boat', term: 'Bateau', description: 'Travèse, vwayaj lwen.', numbers: ['16', '43'] },
  { id: 'boeuf', letter: 'B', icon: 'agriculture', term: 'Bœuf', description: 'Fòs travay, labè.', numbers: ['23', '76'] },
  { id: 'chat', letter: 'C', icon: 'cruelty_free', term: 'Chat', description: 'Entisyon, mistè, vijilans.', numbers: ['11', '39'] },
  { id: 'cheval', letter: 'C', icon: 'directions_run', term: 'Cheval', description: 'Libète, vitès, pisans.', numbers: ['18', '63'] },
  { id: 'chien', letter: 'C', icon: 'pets', term: 'Chien', description: 'Fidèlite, fanmi, vijilans.', numbers: ['12', '98'] },
  { id: 'couteau', letter: 'C', icon: 'content_cut', term: 'Couteau', description: 'Tranchan, separasyon, desizyon.', numbers: ['02', '57'] },
  { id: 'dent', letter: 'D', icon: 'dentistry', term: 'Dent', description: 'Fòs, pawòl, sante.', numbers: ['09', '44'] },
  { id: 'diable', letter: 'D', icon: 'whatshot', term: 'Diable', description: 'Tantasyon, tès, prezans ki fò.', numbers: ['66', '13'] },
  { id: 'eau', letter: 'E', icon: 'water_drop', term: 'Eau', description: 'Mouvman, pasaj, renouvelman.', numbers: ['45', '01'] },
  { id: 'enfant', letter: 'E', icon: 'face', term: 'Enfant', description: 'Inosan, kòmansman, espwa.', numbers: ['10', '28'] },
  { id: 'etoile', letter: 'E', icon: 'star', term: 'Étoile', description: 'Wèl, gidans, destin.', numbers: ['07', '77'] },
  { id: 'feu', letter: 'F', icon: 'local_fire_department', term: 'Feu', description: 'Transfòmasyon, enèji, alèt.', numbers: ['14', '50'] },
  { id: 'fleur', letter: 'F', icon: 'local_florist', term: 'Fleur', description: 'Bote, fèt, kwasans.', numbers: ['06', '35'] },
  { id: 'grenouille', letter: 'G', icon: 'pest_control', term: 'Grenouille', description: 'Transfòmasyon, lapli, feritilite.', numbers: ['17', '73'] },
  { id: 'homme', letter: 'H', icon: 'person', term: 'Homme', description: 'Fòs, direksyon, otorite.', numbers: ['20', '64'] },
  { id: 'jardin', letter: 'J', icon: 'yard', term: 'Jardin', description: 'Kiltivasyon, fanmi, abondans.', numbers: ['03', '38'] },
  { id: 'lapin', letter: 'L', icon: 'cruelty_free', term: 'Lapin', description: 'Rapid, chans, feritilite.', numbers: ['15', '52'] },
  { id: 'lune', letter: 'L', icon: 'nightlight', term: 'Lune', description: 'Sik, entisyon, mistè.', numbers: ['07', '40'] },
  { id: 'maison', letter: 'M', icon: 'home', term: 'Maison', description: 'Sekirite, fanmi, fondman.', numbers: ['19', '61'] },
  { id: 'mer', letter: 'M', icon: 'waves', term: 'Mer', description: 'Abondans, pwofondè, vwayaj.', numbers: ['25', '80'] },
  { id: 'montagne', letter: 'M', icon: 'landscape', term: 'Montagne', description: 'Obstakl, defi, elvasyou.', numbers: ['31', '58'] },
  { id: 'nuit', letter: 'N', icon: 'dark_mode', term: 'Nuit', description: 'Sekrè, repo, tranzisyon.', numbers: ['13', '46'] },
  { id: 'oiseau', letter: 'O', icon: 'flutter_dash', term: 'Oiseau', description: 'Libète, mesaj, leje.', numbers: ['05', '34'] },
  { id: 'or', letter: 'O', icon: 'emoji_events', term: 'Or', description: 'Richès, siksè, limyè.', numbers: ['27', '83'] },
  { id: 'pluie', letter: 'P', icon: 'water', term: 'Pluie', description: 'Benediksyon, pifikasyon, kwasans.', numbers: ['22', '69'] },
  { id: 'poisson', letter: 'P', icon: 'set_meal', term: 'Poisson', description: 'Abondans, pwofondè, dlo.', numbers: ['30', '77'] },
  { id: 'riviere', letter: 'R', icon: 'kayaking', term: 'Rivière', description: 'Ekoulman, chanjman, pasaj.', numbers: ['26', '74'] },
  { id: 'rose', letter: 'R', icon: 'local_florist', term: 'Rose', description: 'Lanmou, bote, rèl kache.', numbers: ['36', '89'] },
  { id: 'serpent', letter: 'S', icon: 'pest_control', term: 'Serpent', description: 'Sajès, danje, transfòmasyon.', numbers: ['24', '68'] },
  { id: 'soleil', letter: 'S', icon: 'wb_sunny', term: 'Soleil', description: 'Klète, enèji, siksè.', numbers: ['32', '79'] },
  { id: 'tigre', letter: 'T', icon: 'cruelty_free', term: 'Tigre', description: 'Pisans, ensten, alèt.', numbers: ['29', '85'] },
  { id: 'tonnerre', letter: 'T', icon: 'thunderstorm', term: 'Tonnerre', description: 'Sipriz, fòs, avètisman.', numbers: ['37', '91'] },
  { id: 'vache', letter: 'V', icon: 'agriculture', term: 'Vache', description: 'Nouri, stabilite, feritilite.', numbers: ['41', '87'] },
  { id: 'voyage', letter: 'V', icon: 'airplane_ticket', term: 'Voyage', description: 'Deplaseman, chanjman, demarsh.', numbers: ['04', '82'] },
];

const ALL_LETTERS = [...new Set(TCHALA_ENTRIES.map(e => e.letter))].sort();

function normalizeQuery(value: string): string {
  return value
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .trim()
    .toLowerCase();
}

@Component({
  selector: 'tch-public-tchala-page',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="tchala-page">
      <section class="tchala-page__hero">
        <p class="tchala-page__eyebrow">{{ 'public.tchala.eyebrow' | tchLabel }}</p>
        <h1>{{ 'public.tchala.title' | tchLabel }}</h1>
        <p class="tchala-page__lead">{{ 'public.tchala.subtitle' | tchLabel }}</p>
      </section>

      <div class="tchala-page__search-wrap">
        <label class="tchala-page__search">
          <span class="tchala-page__search-label">{{ 'public.tchala.search_label' | tchLabel }}</span>
          <input
            type="search"
            class="tchala-page__search-input"
            [placeholder]="'public.tchala.search_placeholder' | tchLabel"
            [value]="query()"
            (input)="updateQuery($event)"
          />
          <span class="material-symbols-outlined tchala-page__search-icon" aria-hidden="true">auto_awesome</span>
        </label>
      </div>

      <nav class="tchala-page__alpha-nav" [attr.aria-label]="'public.tchala.alpha_nav_aria' | tchLabel">
        <button
          type="button"
          class="tchala-page__alpha-pill"
          [class.tchala-page__alpha-pill--active]="activeLetter() === ''"
          (click)="setActiveLetter('')"
        >{{ 'public.tchala.all_label' | tchLabel }}</button>
        @for (letter of letters; track letter) {
          <button
            type="button"
            class="tchala-page__alpha-pill"
            [class.tchala-page__alpha-pill--active]="activeLetter() === letter"
            (click)="setActiveLetter(letter)"
          >{{ letter }}</button>
        }
      </nav>

      @if (filteredEntries().length > 0) {
        <div class="tchala-page__grid" role="list">
          @for (entry of filteredEntries(); track entry.id) {
            <article class="tchala-page__card" role="listitem">
              <div class="tchala-page__card-icon">
                <span class="material-symbols-outlined" aria-hidden="true">{{ entry.icon }}</span>
              </div>
              <div class="tchala-page__card-body">
                <h2 class="tchala-page__card-term">{{ entry.term }}</h2>
                <p class="tchala-page__card-desc">{{ entry.description }}</p>
              </div>
              <div class="tchala-page__numbers" [attr.aria-label]="'public.tchala.numbers_label' | tchLabel">
                @for (num of entry.numbers; track num) {
                  <span>{{ num }}</span>
                }
              </div>
            </article>
          }
        </div>
      } @else {
        <p class="tchala-page__empty">{{ 'public.tchala.empty' | tchLabel }}</p>
      }

      <section class="tchala-page__note" aria-labelledby="tchala-note-title">
        <span class="material-symbols-outlined tchala-page__note-icon" aria-hidden="true">info</span>
        <div>
          <h2 id="tchala-note-title">{{ 'public.tchala.note_title' | tchLabel }}</h2>
          <p>{{ 'public.tchala.note_body' | tchLabel }}</p>
        </div>
      </section>
    </div>
  `,
  styles: [
    `
      .tchala-page {
        display: grid;
        gap: clamp(1.5rem, 4vw, 2.5rem);
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 960px);
        margin: 0 auto;
        padding: clamp(1.5rem, 5vw, 3rem) 0 5rem;
      }

      .tchala-page__hero,
      .tchala-page__note {
        display: grid;
        gap: 0.75rem;
      }

      .tchala-page h1,
      .tchala-page h2,
      .tchala-page p {
        margin: 0;
      }

      .tchala-page__eyebrow {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .tchala-page h1 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
        font-weight: 800;
      }

      .tchala-page__lead {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        max-width: 52ch;
      }

      .tchala-page__search {
        position: relative;
        display: grid;
        align-items: center;
      }

      .tchala-page__search-label {
        position: absolute;
        width: 1px;
        height: 1px;
        overflow: hidden;
        clip: rect(0 0 0 0);
      }

      .tchala-page__search-input {
        min-height: var(--tch-touch-target, 48px);
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        padding: 0 3rem 0 1rem;
        font: inherit;
        width: 100%;
      }

      .tchala-page__search-icon {
        position: absolute;
        right: 1rem;
        color: var(--tch-color-accent, var(--mat-sys-tertiary));
        pointer-events: none;
      }

      .tchala-page__alpha-nav {
        display: flex;
        flex-wrap: wrap;
        gap: 0.375rem;
        overflow-x: auto;
        padding-bottom: 0.25rem;
      }

      .tchala-page__alpha-pill {
        min-height: 2rem;
        min-width: 2rem;
        padding: 0 0.625rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        cursor: pointer;
        font: inherit;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        white-space: nowrap;
        transition: background 0.15s, color 0.15s, border-color 0.15s;
      }

      .tchala-page__alpha-pill--active {
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
        border-color: var(--tch-color-accent, var(--mat-sys-tertiary));
      }

      .tchala-page__grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: var(--tch-page-gutter, 16px);
      }

      .tchala-page__card {
        display: grid;
        gap: 0.5rem;
        padding: 1rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-bottom: 3px solid var(--tch-color-accent, var(--mat-sys-tertiary));
      }

      .tchala-page__card-icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 2.5rem;
        height: 2.5rem;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-primary-fixed, var(--mat-sys-primary-container));
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .tchala-page__card-term {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-title-md, 1rem);
        font-weight: 800;
        line-height: 1.25;
      }

      .tchala-page__card-desc {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        line-height: 1.4;
      }

      .tchala-page__numbers {
        display: flex;
        flex-wrap: wrap;
        gap: 0.375rem;
      }

      .tchala-page__numbers span {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 2.25rem;
        min-height: 2rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
        font-family: var(--tch-font-family-mono, monospace);
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
      }

      .tchala-page__empty {
        padding: 2rem 1rem;
        text-align: center;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .tchala-page__note {
        grid-template-columns: auto 1fr;
        align-items: flex-start;
        padding: 1rem 1.25rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }

      .tchala-page__note-icon {
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .tchala-page__note h2 {
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-title-md, 1rem);
        font-weight: 700;
      }

      .tchala-page__note p {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        line-height: 1.5;
        margin-top: 0.25rem;
      }

      @media (min-width: 760px) {
        .tchala-page h1 {
          font-size: var(--tch-font-size-display-lg, 2.5rem);
          line-height: var(--tch-line-height-display-lg, 3rem);
        }

        .tchala-page__grid {
          grid-template-columns: repeat(3, 1fr);
        }
      }
    `,
  ],
})
export class PublicTchalaPage {
  readonly letters = ALL_LETTERS;
  readonly query = signal('');
  readonly activeLetter = signal('');

  readonly filteredEntries = computed(() => {
    const q = normalizeQuery(this.query());
    const letter = this.activeLetter();

    return TCHALA_ENTRIES.filter(entry => {
      const matchesLetter = !letter || entry.letter === letter;
      if (!matchesLetter) return false;
      if (!q) return true;

      const haystack = normalizeQuery(entry.term + ' ' + entry.description);
      return haystack.includes(q);
    });
  });

  updateQuery(event: Event): void {
    this.query.set(event.target instanceof HTMLInputElement ? event.target.value : '');
    this.activeLetter.set('');
  }

  setActiveLetter(letter: string): void {
    this.activeLetter.set(letter);
    this.query.set('');
  }
}
