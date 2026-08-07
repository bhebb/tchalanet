import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

type NewsFilter = 'all' | 'internal' | 'external';
type PublicContentSourceType = 'INTERNAL' | 'EXTERNAL_RSS';

interface PublicNewsItem {
  readonly id: string;
  readonly title: string;
  readonly content: string | null;
  readonly imageUrl: string | null;
  readonly sourceUrl: string | null;
  readonly sourceType: PublicContentSourceType;
  readonly publishedAt: string | null;
}

const FILTER_OPTIONS: readonly { id: NewsFilter; labelKey: string }[] = [
  { id: 'all', labelKey: 'public.news.filter_all' },
  { id: 'internal', labelKey: 'public.news.filter_internal' },
  { id: 'external', labelKey: 'public.news.filter_external' },
];

@Component({
  selector: 'tch-public-news-page',
  imports: [DatePipe, TranslatePipe, TchErrorPanel, TchLoading],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-news.page.html',
  styleUrls: ['./public-news.page.scss'],
})
export class PublicNewsPage implements OnInit {
  private readonly backend = inject(TchBackendClient);
  private readonly translate = inject(TranslateService);

  readonly filters = FILTER_OPTIONS;
  readonly activeFilter = signal<NewsFilter>('all');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly items = signal<PublicNewsItem[]>([]);

  readonly filteredItems = computed(() => {
    const filter = this.activeFilter();
    const items = [...this.items()].sort((a, b) => {
      if (a.sourceType !== b.sourceType) return a.sourceType === 'INTERNAL' ? -1 : 1;
      return new Date(b.publishedAt ?? 0).getTime() - new Date(a.publishedAt ?? 0).getTime();
    });

    if (filter === 'internal') return items.filter(item => item.sourceType === 'INTERNAL');
    if (filter === 'external') return items.filter(item => item.sourceType === 'EXTERNAL_RSS');
    return items;
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.backend
      .get<PublicNewsItem[]>('/public/news?surface=PUBLIC_HOME&limit=50')
      .subscribe({
        next: items => {
          this.items.set(items ?? []);
          this.loading.set(false);
        },
        error: err => {
          this.error.set(
            (err as { error?: { title?: string; detail?: string } })?.error?.title
              ?? (err as { error?: { detail?: string } })?.error?.detail
              ?? this.translate.instant('public.news.error_fallback'),
          );
          this.loading.set(false);
        },
      });
  }

  setFilter(id: NewsFilter): void {
    this.activeFilter.set(id);
  }

  sourceLabelKey(item: PublicNewsItem): string {
    return item.sourceType === 'INTERNAL' ? 'public.news.source_internal' : 'public.news.source_external';
  }

  sourceIcon(item: PublicNewsItem): string {
    return item.sourceType === 'INTERNAL' ? 'campaign' : 'rss_feed';
  }
}
