import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PlansPage } from './plans.page';

describe('PlansPage', () => {
  let component: PlansPage;
  let fixture: ComponentFixture<PlansPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlansPage],
    }).compileComponents();

    fixture = TestBed.createComponent(PlansPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
