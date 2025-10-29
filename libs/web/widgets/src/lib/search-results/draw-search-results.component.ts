import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DrawSearchResult, SearchService } from './search.service';

@Component({
  selector: 'app-draw-search-results',
  imports: [RouterLink],
  standalone: true,
  template: `
    <div>
      @for (result of searchResults; track result.id) {
      <h3>{{ result.title }}</h3>
      <p>{{ result.description }}</p>
      <button [routerLink]="result.links?.self">Détails</button>
      }
    </div>
  `,
})
export class DrawSearchResultsComponent {
  searchResults: DrawSearchResult[] = [];

  constructor(private searchService: SearchService) {}

  async search(query: string) {
    this.searchResults = await this.searchService.searchDraws(query);
  }
}
