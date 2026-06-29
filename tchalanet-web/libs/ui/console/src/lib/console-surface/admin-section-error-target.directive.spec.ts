import '@angular/compiler';

import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { AdminSectionCardComponent } from './admin-section-card.component';
import {
  AdminSectionErrorTargetDirective,
  AdminSectionTargetError,
} from './admin-section-error-target.directive';

@Component({
  standalone: true,
  imports: [AdminSectionCardComponent, AdminSectionErrorTargetDirective],
  template: `
    <tch-admin-section-card
      title="Commissions"
      tchSectionErrorTarget="dashboard.commissions"
      [tchSectionErrors]="sectionErrors"
    >
      <p>Commission content</p>
    </tch-admin-section-card>
  `,
})
class HostComponent {
  sectionErrors: AdminSectionTargetError[] = [
    {
      target: 'dashboard.commissions',
      severity: 'warn',
      title: 'Commissions indisponibles',
      message: 'Le reste du tableau de bord reste disponible.',
    },
  ];
}

describe('AdminSectionErrorTargetDirective', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(HostComponent);
  });

  it('renders the matching section error in the section card', () => {
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Commissions indisponibles');
    expect(text).toContain('Le reste du tableau de bord reste disponible.');
  });

  it('clears the card error when the target has no matching error', () => {
    fixture.detectChanges();

    const directive = fixture.debugElement
      .query(By.directive(AdminSectionErrorTargetDirective))
      .injector.get(AdminSectionErrorTargetDirective);
    const card = fixture.debugElement
      .query(By.directive(AdminSectionCardComponent))
      .injector.get(AdminSectionCardComponent);

    directive.errors = [
      {
        target: 'dashboard.readiness',
        severity: 'error',
        title: 'Readiness indisponible',
        message: 'Ce bloc ne peut pas etre charge.',
      },
    ];
    directive.ngDoCheck();

    expect(card.sectionError()).toBeNull();
  });
});
