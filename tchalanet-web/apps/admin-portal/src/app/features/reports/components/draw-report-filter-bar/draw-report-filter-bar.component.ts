import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { TchMultiSearchSelect } from '@tch/ui/components';
import type { TchSearchOption } from '@tch/ui/components';
import { Observable } from 'rxjs';

export interface DrawReportSortOption {
  readonly value: string;
  readonly labelKey: string;
}

@Component({
  selector: 'tch-draw-report-filter-bar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    MatFormFieldModule,
    MatInputModule,
    MatOptionModule,
    MatSelectModule,
    TchMultiSearchSelect,
  ],
  templateUrl: './draw-report-filter-bar.component.html',
  styleUrls: ['./draw-report-filter-bar.component.scss'],
})
export class DrawReportFilterBarComponent {
  readonly from = input('');
  readonly to = input('');
  readonly sort = input.required<string>();
  readonly sortOptions = input.required<readonly DrawReportSortOption[]>();
  readonly searchDraws = input.required<(query: string) => Observable<readonly TchSearchOption[]>>();

  readonly fromChange = output<string>();
  readonly toChange = output<string>();
  readonly sortChange = output<string>();
  readonly drawsChange = output<readonly TchSearchOption[]>();
}
