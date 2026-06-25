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
  { id: 'all',     labelKey: 'common.all' },
  { id: 'results', labelKey: 'domain.entity.results' },
  { id: 'tchala',  labelKey: 'public.news.filter_tchala' },
  { id: 'system',  labelKey: 'public.news.filter_system' },
  { id: 'promo',   labelKey: 'domain.entity.promotions' },
];

@Component({
  selector: 'tch-public-news-page',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-news.page.html',
  styleUrls: ['./public-news.page.scss'],
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
