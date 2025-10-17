import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PrivateShellComponent } from './private-shell.component';

describe('ShellComponent', () => {
  let component: PrivateShellComponent;
  let fixture: ComponentFixture<PrivateShellComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrivateShellComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PrivateShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
