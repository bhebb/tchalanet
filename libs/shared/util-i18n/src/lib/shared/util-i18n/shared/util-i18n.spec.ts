import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SharedUtilI18n } from './util-i18n';

describe('SharedUtilI18n', () => {
  let component: SharedUtilI18n;
  let fixture: ComponentFixture<SharedUtilI18n>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SharedUtilI18n],
    }).compileComponents();

    fixture = TestBed.createComponent(SharedUtilI18n);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
