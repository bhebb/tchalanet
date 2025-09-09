import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PlanItem } from './plan-item';

describe('PlanItem', () => {
  let component: PlanItem;
  let fixture: ComponentFixture<PlanItem>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlanItem],
    }).compileComponents();

    fixture = TestBed.createComponent(PlanItem);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
