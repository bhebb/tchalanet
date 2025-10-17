import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DrawsPage } from './draws.page';

describe('DrawsPage', () => {
  let component: DrawsPage;
  let fixture: ComponentFixture<DrawsPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DrawsPage],
    }).compileComponents();

    fixture = TestBed.createComponent(DrawsPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
