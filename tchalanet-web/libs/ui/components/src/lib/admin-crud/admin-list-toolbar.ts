import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';

export interface AdminFilter {
  readonly code: string;
  readonly label: string;
}

@Component({
  selector: 'tch-admin-list-toolbar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatFormFieldModule, MatInputModule, MatIconModule],
  template: `
    <div class="list-toolbar">
      <mat-form-field appearance="outline" class="list-toolbar__search" subscriptSizing="dynamic">
        <mat-icon matPrefix>search</mat-icon>
        <input
          matInput
          type="search"
          [placeholder]="searchPlaceholder()"
          [value]="searchValue()"
          (input)="onSearch($event)"
        />
      </mat-form-field>

      <div class="list-toolbar__extra">
        <ng-content />
      </div>
    </div>

    @if (filters().length) {
      <div class="list-toolbar__filters tch-status-tabs" role="group" [attr.aria-label]="filtersLabel()">
        @for (f of filters(); track f.code) {
          <button
            type="button"
            class="tch-status-tab"
            [class.tch-status-tab--active]="activeFilter() === f.code"
            (click)="filterChange.emit(f.code)"
          >
            {{ f.label }}
          </button>
        }
      </div>
    }
  `,
  styles: [
    `
      :host {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }

      .list-toolbar {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        flex-wrap: wrap;
      }

      .list-toolbar__search {
        flex: 1;
        min-width: 200px;
        max-width: 340px;
      }

      .list-toolbar__extra {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        flex-shrink: 0;
      }
    `,
  ],
})
export class AdminListToolbar {
  readonly searchPlaceholder = input('Rechercher…');
  readonly searchValue = input('');
  readonly filters = input<readonly AdminFilter[]>([]);
  readonly activeFilter = input('ALL');
  readonly filtersLabel = input('Filtrer par statut');

  readonly searchChange = output<string>();
  readonly filterChange = output<string>();

  onSearch(event: Event): void {
    this.searchChange.emit((event.target as HTMLInputElement).value);
  }
}
