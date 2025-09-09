import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PageFacade } from '@tchl/facades';
import { GridLayoutComponent } from '@tchl/ui/layout';

@Component({
  selector: 'tchl-home-public-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatProgressSpinnerModule, GridLayoutComponent],
  template: `
    @if (pageData(); as pageModel) {
    <tchl-grid-layout [layout]="pageModel.layout" />
    } @else {
    <p>Loading layout...</p>
    }
  `,
  styles: [],
})
export class HomePublicPage {
  private readonly pageFacade = inject(PageFacade);

  pageData = this.pageFacade.page;
  isLoading = this.pageFacade.loading;
}
