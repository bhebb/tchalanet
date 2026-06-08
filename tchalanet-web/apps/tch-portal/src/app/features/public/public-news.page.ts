import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';



type NewsCategory = 'all' | 'results' | 'tchala' | 'system' | 'promo' | 'tips';

interface NewsItem {
  readonly id: string;
  readonly category: Exclude<NewsCategory, 'all'>;
  readonly categoryLabel: string;
  readonly categoryStyle: 'primary' | 'secondary' | 'neutral';
  readonly title: string;
  readonly excerpt: string;
  readonly time: string;
  readonly icon: string;
  readonly accent?: 'primary' | 'secondary';
}

const NEWS_ITEMS: readonly NewsItem[] = [
  {
    id: 'ny-45',
    category: 'results',
    categoryLabel: 'NY Afternoon',
    categoryStyle: 'secondary',
    title: 'Résultats New York : Le 45 fait des heureux',
    excerpt: 'Le tirage de cet après-midi a révélé des combinaisons inattendues. Découvrez les gagnants.',
    time: '14:30',
    icon: 'confirmation_number',
    accent: 'secondary',
  },
  {
    id: 'tchala-eau',
    category: 'tchala',
    categoryLabel: 'Tchala',
    categoryStyle: 'primary',
    title: "Rêver d'eau : Quels numéros jouer ?",
    excerpt: "L'interprétation complète de vos rêves de cette nuit selon le dictionnaire Tchala officiel.",
    time: '10:15',
    icon: 'water_drop',
  },
  {
    id: 'securite',
    category: 'system',
    categoryLabel: 'Système',
    categoryStyle: 'neutral',
    title: 'Nouvelle sécurité sur vos transactions',
    excerpt: 'Nous renforçons la validation de vos fiches avec le nouveau sceau numérique sécurisé.',
    time: 'Hier',
    icon: 'security',
  },
  {
    id: 'jackpot-weekend',
    category: 'promo',
    categoryLabel: 'Promotion',
    categoryStyle: 'secondary',
    title: 'Jackpot spécial ce weekend !',
    excerpt: "Multipliez vos gains par 5 sur tous les Lotto 3 du samedi soir. Ne ratez pas l'occasion.",
    time: 'Hier',
    icon: 'emoji_events',
    accent: 'primary',
  },
  {
    id: 'tips-3',
    category: 'tips',
    categoryLabel: 'Conseils',
    categoryStyle: 'neutral',
    title: 'Optimisez vos mises en 3 étapes',
    excerpt: 'Comment utiliser les statistiques de tirage pour mieux choisir vos numéros favoris.',
    time: 'Il y a 2 jours',
    icon: 'lightbulb',
  },
  {
    id: 'fl-midi',
    category: 'results',
    categoryLabel: 'FL Midi',
    categoryStyle: 'secondary',
    title: 'Florida Midi : résultats du tirage',
    excerpt: 'Le 07 et le 33 en tête des combinaisons gagnantes pour le tirage de midi.',
    time: 'Il y a 2 jours',
    icon: 'confirmation_number',
    accent: 'secondary',
  },
];

const FILTER_OPTIONS: readonly { id: NewsCategory; labelKey: string }[] = [
  { id: 'all',     labelKey: 'public.news.filter_all' },
  { id: 'results', labelKey: 'public.news.filter_results' },
  { id: 'tchala',  labelKey: 'public.news.filter_tchala' },
  { id: 'system',  labelKey: 'public.news.filter_system' },
  { id: 'promo',   labelKey: 'public.news.filter_promo' },
];

@Component({
  selector: 'tch-public-news-page',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="news-page">
      <div class="news-page__header">
        <div class="news-page__title-row">
          <h1>{{ 'public.news.title' | translate }}</h1>
          <span class="news-page__live-badge">LIVE</span>
        </div>
        <nav class="news-page__filters" [attr.aria-label]="'public.news.filter_aria' | translate">
          @for (filter of filters; track filter.id) {
            <button
              type="button"
              class="news-page__filter-pill"
              [class.news-page__filter-pill--active]="activeFilter() === filter.id"
              (click)="setFilter(filter.id)"
            >{{ filter.labelKey | translate }}</button>
          }
        </nav>
      </div>

      <div class="news-page__feed" role="feed" [attr.aria-label]="'public.news.feed_aria' | translate">
        @for (item of filteredItems(); track item.id) {
          <article
            class="news-page__article"
            [class.news-page__article--accent-primary]="item.accent === 'primary'"
            [class.news-page__article--accent-secondary]="item.accent === 'secondary'"
          >
            <div class="news-page__thumb" [attr.aria-hidden]="true">
              <span class="material-symbols-outlined">{{ item.icon }}</span>
            </div>
            <div class="news-page__body">
              <div class="news-page__meta">
                <span
                  class="news-page__category"
                  [class.news-page__category--primary]="item.categoryStyle === 'primary'"
                  [class.news-page__category--secondary]="item.categoryStyle === 'secondary'"
                  [class.news-page__category--neutral]="item.categoryStyle === 'neutral'"
                >{{ item.categoryLabel }}</span>
                <span class="news-page__time">{{ item.time }}</span>
              </div>
              <h2 class="news-page__article-title">{{ item.title }}</h2>
              <p class="news-page__excerpt">{{ item.excerpt }}</p>
            </div>
          </article>
        } @empty {
          <p class="news-page__empty">{{ 'public.news.empty' | translate }}</p>
        }
      </div>
    </div>
  `,
  styles: [
    `
      .news-page {
        display: grid;
        gap: 1.5rem;
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 720px);
        margin: 0 auto;
        padding: clamp(1.5rem, 5vw, 2.5rem) 0 5rem;
      }

      .news-page__header {
        display: grid;
        gap: 1rem;
      }

      .news-page__title-row {
        display: flex;
        align-items: center;
        gap: 0.75rem;
      }

      .news-page__title-row h1 {
        margin: 0;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
        font-weight: 800;
      }

      .news-page__live-badge {
        padding: 0.125rem 0.625rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-primary-fixed, var(--mat-sys-primary-container));
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        letter-spacing: 0.05em;
      }

      .news-page__filters {
        display: flex;
        gap: 0.5rem;
        overflow-x: auto;
        padding-bottom: 0.25rem;
        scrollbar-width: none;
      }

      .news-page__filters::-webkit-scrollbar { display: none; }

      .news-page__filter-pill {
        white-space: nowrap;
        min-height: 2rem;
        padding: 0 1rem;
        border-radius: var(--tch-radius-pill, 9999px);
        border: none;
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        cursor: pointer;
        font: inherit;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        transition: background 0.15s, color 0.15s;
      }

      .news-page__filter-pill--active {
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }

      .news-page__feed {
        display: grid;
        gap: 0.75rem;
      }

      .news-page__article {
        display: flex;
        gap: 1rem;
        padding: 1rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        border: 1px solid transparent;
        transition: background 0.15s;
        cursor: pointer;
      }

      .news-page__article:hover {
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
      }

      .news-page__article--accent-secondary {
        border-left: 4px solid var(--tch-color-accent, var(--mat-sys-tertiary));
      }

      .news-page__article--accent-primary {
        border-left: 4px solid var(--tch-color-primary, var(--mat-sys-primary));
      }

      .news-page__thumb {
        flex-shrink: 0;
        display: grid;
        place-items: center;
        width: 5rem;
        height: 5rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .news-page__thumb .material-symbols-outlined {
        font-size: 2rem;
      }

      .news-page__body {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
        min-width: 0;
      }

      .news-page__meta {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.5rem;
      }

      .news-page__category {
        font-family: var(--tch-font-family-mono, monospace);
        font-size: 0.625rem;
        font-weight: 800;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        padding: 0.125rem 0.375rem;
        border-radius: var(--tch-radius-sm, 4px);
      }

      .news-page__category--primary {
        background: var(--tch-color-primary-fixed, var(--mat-sys-primary-container));
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .news-page__category--secondary {
        color: var(--tch-color-secondary, var(--mat-sys-secondary));
        background: transparent;
      }

      .news-page__category--neutral {
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .news-page__time {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-outline, var(--mat-sys-outline));
        white-space: nowrap;
      }

      .news-page__article-title {
        margin: 0;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-title-md, 1rem);
        font-weight: 700;
        line-height: 1.3;
        overflow: hidden;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
      }

      .news-page__excerpt {
        margin: 0;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        line-height: 1.4;
        overflow: hidden;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
      }

      .news-page__empty {
        padding: 2rem;
        text-align: center;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
      }

      @media (min-width: 760px) {
        .news-page__title-row h1 {
          font-size: var(--tch-font-size-display-lg, 2.5rem);
          line-height: var(--tch-line-height-display-lg, 3rem);
        }
      }
    `,
  ],
})
export class PublicNewsPage {
  readonly filters = FILTER_OPTIONS;
  readonly activeFilter = signal<NewsCategory>('all');

  readonly filteredItems = computed(() => {
    const f = this.activeFilter();
    return f === 'all' ? NEWS_ITEMS : NEWS_ITEMS.filter(item => item.category === f);
  });

  setFilter(id: NewsCategory): void {
    this.activeFilter.set(id);
  }
}
