import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../shared/admin-ui/admin-status-pill.component';
import { AdminTicketsApi, TicketRowView, TicketStatus } from '../../admin-tickets-api.service';

@Component({
  selector: 'tch-admin-tickets-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    AdminPageShellComponent,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './admin-tickets.page.html',
  styleUrls: ['./admin-tickets.page.scss'],
})
export class AdminTicketsPage implements OnInit {
  private readonly api = inject(AdminTicketsApi);

  readonly columns = ['ticketCode', 'status', 'drawChannelName', 'drawScheduledAt', 'totalAmountCents', 'placedAt'];

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly items = signal<TicketRowView[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly statusFilter = signal<TicketStatus | ''>('');

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / 20)));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list({
      status: this.statusFilter() || undefined,
      page: this.page(),
      size: 20,
    }).subscribe({
      next: p => {
        this.items.set(p.content);
        this.total.set(p.totalElements);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  onStatusFilter(status: TicketStatus | ''): void {
    this.statusFilter.set(status);
    this.page.set(0);
    this.load();
  }

  prevPage(): void {
    if (this.page() > 0) { this.page.update(p => p - 1); this.load(); }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) { this.page.update(p => p + 1); this.load(); }
  }

  statusTone(status: TicketStatus): AdminStatusTone {
    switch (status) {
      case 'PLACED': return 'neutral';
      case 'PAID': return 'success';
      case 'CANCELLED': return 'danger';
      case 'EXPIRED': return 'warning';
      default: return 'neutral';
    }
  }

  amountDisplay(cents: number): string {
    return (cents / 100).toFixed(2);
  }
}
