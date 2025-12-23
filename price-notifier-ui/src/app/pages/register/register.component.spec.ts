import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RegisterComponent } from './register.component';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { of, throwError } from 'rxjs';
import {REGISTER_CONSTANTS} from '../../core/constants/register.constants';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;

  const authServiceMock = {
    register: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty form signals', () => {
    expect(component.firstName()).toBe('');
    expect(component.email()).toBe('');
    expect(component.loading()).toBe(false);
  });

  it('should show error if required fields are missing', () => {
    component.email.set('');
    component.password.set('');

    // Act
    component.onSubmit();

    // Assert
    expect(component.errorMessage()).toBe(REGISTER_CONSTANTS.MESSAGES.ERRORS.INVALID_FORM);
    expect(component.loading()).toBe(false);
    expect(authServiceMock.register).not.toHaveBeenCalled();
  });

  it('should call authService.register on valid submit', () => {
    // Arrange
    const testData = {
      firstName: 'Jan',
      lastName: 'Kowalski',
      email: 'jan@test.pl',
      password: 'securePassword'
    };

    component.firstName.set(testData.firstName);
    component.lastName.set(testData.lastName);
    component.email.set(testData.email);
    component.password.set(testData.password);

    authServiceMock.register.mockReturnValue(of({}));

    // Act
    component.onSubmit();

    // Assert
    expect(component.loading()).toBe(false);
    expect(authServiceMock.register).toHaveBeenCalledWith(testData);
  });

  it('should handle registration error', () => {
    // Arrange
    component.email.set('taken@email.com');
    component.password.set('123456');

    authServiceMock.register.mockReturnValue(throwError(() => new Error('Email exists')));

    // Act
    component.onSubmit();

    // Assert
    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toBe(REGISTER_CONSTANTS.MESSAGES.ERRORS.GENERIC);
  });
});
