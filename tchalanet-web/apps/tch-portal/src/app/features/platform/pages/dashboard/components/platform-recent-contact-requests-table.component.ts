import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { AdminStatusPillComponent } from '../../../../private/shared/admin-ui/admin-status-pill.component';
import { AdminStatusTone } from '../../../../private/shared/admin-ui/admin-status-pill.component';
import { PlatformRecentContactRequestItem } from '../platform-dashboard.model';

function contactStatusTone(
  status: PlatformRecentContactRequestItem['status'],
): AdminStatusTone {
  switch (status) {
    case 'RECEIVED':
      return 'info';
    case 'PENDING':
      return 'warning';
    case 'PROCESSING':
      return 'neutral';
    case 'CLOSED':
      return 'success';
  }
}

@Component({
  selector: 'tch-platform-recent-contact-requests-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminStatusPillComponent],
  template: `
    @if (items().length) {
      <div class="contacts-table-wrap">
        <table class="contacts-table">
          <thead>
            <tr>
              <th>Requester</th>
              <th>Subject</th>
              <th>Date</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            @for (item of items(); track item.reference) {
              <tr>
                <td>
                  <p class="contacts-table__name">{{ item.requesterName }}</p>
                  @if (item.requesterLabel) {
                    <p class="contacts-table__label">{{ item.requesterLabel }}</p>
                  }
                </td>
                <td>{{ item.subject }}</td>
                <td class="contacts-table__date">{{ item.createdAtLabel }}</td>
                <td>
                  <tch-admin-status-pill
                    [label]="item.status"
                    [tone]="statusTone(item.status)"
                  />
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    } @else {
      <p class="contacts-table--empty">No recent contact requests.</p>
    }
  `,
  styles: [
    `
      .contacts-table-wrap {
        overflow-x: auto;
        margin: -1.5rem;
      }

      .contacts-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.875rem;
      }

      .contacts-table th {
        padding: 0.75rem 1.5rem;
        text-align: left;
        font-size: 0.6875rem;
        font-weight: 700;
        letter-spacing: 0.06em;
        text-transform: uppercase;
        color: var(--tch-color-on-surface-variant, #46464f);
        background: var(--tch-color-surface-container-low, #f3f3f6);
        border-bottom: 1px solid var(--tch-color-outline-variant, #c8c5d0);
      }

      .contacts-table td {
        padding: 0.875rem 1.5rem;
        color: var(--tch-color-on-surface, #1a1c1e);
        border-bottom: 1px solid color-mix(
          in srgb,
          var(--tch-color-outline-variant, #c8c5d0) 50%,
          transparent
        );
        vertical-align: middle;
      }

      .contacts-table tbody tr:last-child td {
        border-bottom: none;
      }

      .contacts-table tbody tr:hover td {
        background: var(--tch-color-surface-container-low, #f3f3f6);
      }

      .contacts-table__name {
        margin: 0;
        font-weight: 600;
      }

      .contacts-table__label {
        margin: 0;
        font-size: 0.75rem;
        color: var(--tch-color-on-surface-variant, #46464f);
      }

      .contacts-table__date {
        font-family: 'JetBrains Mono', monospace;
        font-size: 0.8125rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        white-space: nowrap;
      }

      .contacts-table--empty {
        margin: 0;
        font-size: 0.875rem;
        color: var(--tch-color-on-surface-variant, #46464f);
      }
    `,
  ],
})
export class PlatformRecentContactRequestsTableComponent {
  readonly items = input.required<readonly PlatformRecentContactRequestItem[]>();

  statusTone(status: PlatformRecentContactRequestItem['status']): AdminStatusTone {
    return contactStatusTone(status);
  }
}
