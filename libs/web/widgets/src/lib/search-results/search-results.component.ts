import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SearchService } from './search.service';

@Component({
  selector: 'tch-results-page',
  standalone: true,
  template: `
    <section class="h-container tch-results">
      <div class="tch-results__controls">
        <div id="searchbox"></div>
        <div id="clear"></div>
        <div id="tags"></div>
        <div id="lang"></div>
        <div id="sort"></div>
      </div>
      <div id="hits"></div>
      <div id="pagination"></div>
    </section>
  `,
})
export class SearchResultsComponent {
  private route = inject(ActivatedRoute);

  constructor(private searchService: SearchService) {}

  async search(query: string) {
    // Recherche de base
    // const results = await this.searchService.performSearch(query);

    // // Recherche avec options
    // const filteredResults = await this.searchService.performSearch(query, {
    //   indexName: 'books',
    //   limit: 20,
    //   filter: 'category="science"',
    // });

    // Suggestions
    // const suggestions = await this.searchService.getSuggestions(query);
  }
}
