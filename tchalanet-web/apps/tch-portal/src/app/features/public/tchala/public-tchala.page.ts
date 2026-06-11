import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';

import { TchPage } from '@tch/api';
import {
  PublicEmptyStateComponent,
  PublicFilterBarComponent,
  PublicListShellComponent,
  PublicPaginationBarComponent,
} from '../shared';
import { PublicTchalaService, TchalaEntry, TchalaSuggestionRequest } from './public-tchala.service';
import type { FormState, TchalaDisplayEntry } from './public-tchala.model';
import { PAGE_SIZE, apiEntryToDisplay } from './public-tchala.utils';

@Component({
  selector: 'tch-public-tchala-page',
  imports: [
    TranslatePipe,
    PublicListShellComponent,
    PublicFilterBarComponent,
    PublicPaginationBarComponent,
    PublicEmptyStateComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-tchala.page.html',
  styleUrls: ['./public-tchala.page.scss'],
})
export class PublicTchalaPage {
  private readonly tchalaService = inject(PublicTchalaService);

  // ── Catalogue ────────────────────────────────────────────────────────────────

  readonly query = signal('');
  readonly currentPage = signal(0);

  readonly resource = rxResource({
    params: () => ({
      lang: 'ht', // catalogue Tchala en créole haïtien uniquement
      q: this.query().trim() || undefined,
      page: this.currentPage(),
      size: PAGE_SIZE,
    }),
    stream: ({ params }): import('rxjs').Observable<TchPage<TchalaEntry>> =>
      this.tchalaService.search(params.lang, params.q, params.page, params.size),
  });

  readonly entries = computed((): readonly TchalaDisplayEntry[] =>
    (this.resource.value()?.items ?? []).map(apiEntryToDisplay),
  );

  readonly totalPages = computed(() => this.resource.value()?.totalPages ?? 0);

  onQueryInput(event: Event): void {
    this.query.set(event.target instanceof HTMLInputElement ? event.target.value : '');
    this.currentPage.set(0);
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // ── Suggestions ──────────────────────────────────────────────────────────────

  readonly suggestionStatusResource = rxResource({
    params: () => ({}),
    stream: () => this.tchalaService.suggestionStatus(),
  });

  readonly suggestionOpen = computed(() => this.suggestionStatusResource.value()?.open ?? false);

  readonly formExpanded = signal(false);
  readonly dreamInput = signal('');
  readonly numbersInput = signal('');
  readonly noteInput = signal('');
  readonly formState = signal<FormState>('idle');

  readonly canSubmit = computed(
    () => this.dreamInput().trim().length >= 2 && this.numbersInput().trim().length >= 1,
  );

  getInputValue(event: Event): string {
    return event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement
      ? event.target.value
      : '';
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    if (!this.canSubmit() || this.formState() === 'submitting') return;

    this.formState.set('submitting');

    const body: TchalaSuggestionRequest = {
      lang: 'ht',
      dream: this.dreamInput().trim(),
      numbers: this.numbersInput().trim(),
      note: this.noteInput().trim() || undefined,
    };

    this.tchalaService.submitSuggestion(body).subscribe({
      next: () => {
        this.formState.set('success');
        this.formExpanded.set(false);
      },
      error: () => this.formState.set('error'),
    });
  }

  resetForm(): void {
    this.dreamInput.set('');
    this.numbersInput.set('');
    this.noteInput.set('');
    this.formState.set('idle');
    this.formExpanded.set(false);
    this.suggestionStatusResource.reload();
  }
}
