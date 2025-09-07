import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DataAccessConfig } from './data-access-config';

describe('DataAccessConfig', () => {
  let component: DataAccessConfig;
  let fixture: ComponentFixture<DataAccessConfig>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DataAccessConfig],
    }).compileComponents();

    fixture = TestBed.createComponent(DataAccessConfig);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
