import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SharedUiShellMaterial } from './ui-shell-material';

describe('SharedUiShellMaterial', () => {
  let component: SharedUiShellMaterial;
  let fixture: ComponentFixture<SharedUiShellMaterial>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SharedUiShellMaterial],
    }).compileComponents();

    fixture = TestBed.createComponent(SharedUiShellMaterial);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
