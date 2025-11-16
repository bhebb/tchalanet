import { ChangeDetectionStrategy, Component } from '@angular/core';


@Component({
  selector: 'tch-results-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
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
  async search(_: string) {
    // for testing
  }
}
